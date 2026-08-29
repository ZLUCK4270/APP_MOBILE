"""
main.py — Punto de entrada de la API RESTful para ECOLIM S.A.C.
Framework: FastAPI (Python 3.11+)
Servidor: Uvicorn ASGI

Ejecutar:
    pip install -r requirements.txt
    python main.py

Documentación automática:
    Swagger UI: http://localhost:8000/docs
    ReDoc:      http://localhost:8000/redoc
"""

from fastapi import FastAPI, HTTPException, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime, date
from io import BytesIO
import uvicorn
import json
import os

# ============================================================
# CONFIGURACIÓN DE LA APLICACIÓN
# ============================================================

app = FastAPI(
    title="ECOLIM S.A.C. — API de Residuos",
    description="API RESTful para gestión de recolección de residuos sólidos "
                "industriales. Soporta sincronización offline-first desde "
                "dispositivos Android.",
    version="1.0.0",
    contact={"name": "ECOLIM S.A.C.", "email": "admin@ecolim.com"}
)

# Configurar CORS para permitir peticiones desde la app Android
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============================================================
# BASE DE DATOS EN MEMORIA (Simulación de Firestore)
# Para producción: reemplazar con firebase_admin + firestore
# ============================================================

# Almacén en memoria para demo (simula Firestore)
db_residuos = [
    {"id_residuo": 1, "nombre": "Plásticos Peligrosos", "peligrosidad": True},
    {"id_residuo": 2, "nombre": "Metales Pesados", "peligrosidad": True},
    {"id_residuo": 3, "nombre": "Cartón Industrial", "peligrosidad": False},
    {"id_residuo": 4, "nombre": "Químicos", "peligrosidad": True},
]

db_registros = []  # Se llena con sincronización desde dispositivos
db_sync_log = []   # Log de sincronizaciones


# ============================================================
# MODELOS PYDANTIC (Schemas de validación)
# ============================================================

class ResiduoSchema(BaseModel):
    """Esquema de validación para un tipo de residuo."""
    id_residuo: Optional[int] = None
    nombre: str = Field(..., min_length=2,
                        description="Nombre del tipo de residuo")
    peligrosidad: bool = Field(False,
                                description="True si es peligroso según NTP")

class RegistroSchema(BaseModel):
    """Esquema de validación para un registro de recolección."""
    id_registro: Optional[int] = None
    id_usuario: int = Field(..., description="ID del operario")
    id_cliente: int = Field(..., description="ID del cliente/sede")
    id_residuo: int = Field(..., description="ID del tipo de residuo")
    peso_kg: float = Field(..., gt=0, description="Peso en kilogramos")
    volumen_m3: float = Field(..., gt=0, description="Volumen en metros cúbicos")
    observacion: Optional[str] = Field(None, description="Notas del operario")
    fecha_hora: Optional[str] = Field(None, description="Timestamp ISO 8601")

class SyncPayload(BaseModel):
    """Payload que envía el dispositivo Android al sincronizar."""
    dispositivo_id: str = Field(...,
                                 description="ID único del dispositivo Android")
    registros: List[RegistroSchema] = Field(...,
                                             description="Registros pendientes")

class ReporteResumen(BaseModel):
    """Resumen de un reporte por tipo de residuo."""
    peso_total_kg: float = 0
    volumen_total_m3: float = 0
    cantidad_registros: int = 0


# ============================================================
# ENDPOINTS CRUD — /api/v1/residuos
# ============================================================

@app.get("/api/v1/residuos", response_model=List[ResiduoSchema],
         tags=["Residuos"],
         summary="Listar tipos de residuo")
async def listar_residuos():
    """Retorna el catálogo completo de tipos de residuo."""
    return db_residuos


@app.post("/api/v1/residuos", response_model=ResiduoSchema,
          status_code=201, tags=["Residuos"],
          summary="Crear tipo de residuo")
async def crear_residuo(residuo: ResiduoSchema):
    """Agrega un nuevo tipo de residuo al catálogo."""
    # Generar ID auto-incremental
    new_id = max([r["id_residuo"] for r in db_residuos], default=0) + 1
    nuevo = residuo.dict()
    nuevo["id_residuo"] = new_id
    db_residuos.append(nuevo)
    return nuevo


# ============================================================
# ENDPOINTS CRUD — /api/v1/registros
# ============================================================

@app.get("/api/v1/registros", response_model=List[RegistroSchema],
         tags=["Registros"],
         summary="Listar registros de recolección")
async def listar_registros(
    fecha: Optional[str] = None,
    residuo: Optional[int] = None
):
    """Lista registros con filtros opcionales por fecha y tipo de residuo."""
    resultado = db_registros.copy()

    if fecha:
        resultado = [r for r in resultado
                     if r.get("fecha_hora", "").startswith(fecha)]
    if residuo:
        resultado = [r for r in resultado
                     if r.get("id_residuo") == residuo]

    return resultado


@app.post("/api/v1/registros", response_model=RegistroSchema,
          status_code=201, tags=["Registros"],
          summary="Crear registro de recolección")
async def crear_registro(registro: RegistroSchema):
    """Crea un nuevo registro individual de recolección."""
    new_id = max([r.get("id_registro", 0) for r in db_registros], default=0) + 1
    nuevo = registro.dict()
    nuevo["id_registro"] = new_id
    if not nuevo.get("fecha_hora"):
        nuevo["fecha_hora"] = datetime.now().isoformat()
    db_registros.append(nuevo)
    return nuevo


# ============================================================
# ENDPOINT — /api/v1/reportes
# ============================================================

@app.get("/api/v1/reportes", tags=["Reportes"],
         summary="Generar reporte consolidado")
async def generar_reporte(
    fecha_inicio: Optional[str] = None,
    fecha_fin: Optional[str] = None,
    formato: str = "json"
):
    """
    Genera reporte consolidado por periodo.
    Formatos soportados: json, pdf, excel.
    """
    # Filtrar registros por rango de fechas
    registros = db_registros.copy()
    if fecha_inicio:
        registros = [r for r in registros
                     if r.get("fecha_hora", "") >= fecha_inicio]
    if fecha_fin:
        registros = [r for r in registros
                     if r.get("fecha_hora", "") <= fecha_fin + "T23:59:59"]

    # Calcular resumen por tipo de residuo
    resumen = {}
    for reg in registros:
        tipo = str(reg.get("id_residuo", "sin_clasificar"))
        if tipo not in resumen:
            resumen[tipo] = {
                "peso_total_kg": 0,
                "volumen_total_m3": 0,
                "cantidad_registros": 0
            }
        resumen[tipo]["peso_total_kg"] += reg.get("peso_kg", 0)
        resumen[tipo]["volumen_total_m3"] += reg.get("volumen_m3", 0)
        resumen[tipo]["cantidad_registros"] += 1

    reporte = {
        "periodo": {
            "inicio": fecha_inicio or "Sin filtro",
            "fin": fecha_fin or "Sin filtro"
        },
        "total_registros": len(registros),
        "resumen_por_residuo": resumen,
        "generado_en": datetime.now().isoformat()
    }

    return reporte


# ============================================================
# ENDPOINT — /api/v1/sync (Sincronización Offline-First)
# ============================================================

@app.post("/api/v1/sync", tags=["Sincronización"],
          summary="Sincronizar dispositivo")
async def sincronizar_dispositivo(payload: SyncPayload):
    """
    Recibe un lote de registros pendientes desde el dispositivo Android.

    Flujo:
    1. Recibe lista de registros con isSynced=0 desde SQLite local
    2. Valida cada registro con Pydantic
    3. Persiste en la base de datos central
    4. Retorna IDs sincronizados para que el dispositivo marque isSynced=1
    """
    ids_sincronizados = []

    for registro in payload.registros:
        # Crear nuevo registro en la base de datos central
        nuevo = registro.dict()
        nuevo["dispositivo_origen"] = payload.dispositivo_id
        nuevo["sincronizado_en"] = datetime.now().isoformat()

        # Asignar nuevo ID si no tiene
        if not nuevo.get("id_registro"):
            nuevo["id_registro"] = len(db_registros) + 1

        db_registros.append(nuevo)
        ids_sincronizados.append(registro.id_registro)

    # Registrar en log de sincronización
    db_sync_log.append({
        "dispositivo_id": payload.dispositivo_id,
        "cantidad_registros": len(ids_sincronizados),
        "timestamp": datetime.now().isoformat()
    })

    return {
        "status": "OK",
        "mensaje": f"{len(ids_sincronizados)} registros sincronizados",
        "ids_sincronizados": ids_sincronizados,
        "timestamp_servidor": datetime.now().isoformat()
    }


# ============================================================
# ENDPOINT — /api/v1/analisis-foto (Análisis de IA)
# ============================================================

@app.post("/api/v1/analisis-foto", tags=["Inteligencia Artificial"],
          summary="Analizar foto de residuo")
async def analizar_foto(foto: UploadFile = File(...)):
    """
    Recibe una fotografía desde la aplicación Android y utiliza 
    modelos de visión computacional (ej. YOLOv8 o TensorFlow) para 
    clasificar el residuo.
    
    Actualmente es un MOCK que simula el análisis para integración.
    """
    if not foto.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="El archivo no es una imagen")
        
    # Aquí iría el código de procesamiento de la imagen con OpenCV/TensorFlow
    # image_bytes = await foto.read()
    # predicted_class, confidence = mi_modelo.predict(image_bytes)
    
    # --- SIMULACIÓN DE RESPUESTA DE LA IA ---
    # Simulamos que la IA detectó "Plásticos Peligrosos" con 89% de confianza
    return {
        "status": "success",
        "mensaje": "Análisis completado",
        "resultados_ia": {
            "id_residuo_detectado": 1, 
            "nombre_detectado": "Plásticos Peligrosos",
            "confianza_porcentaje": 89.5,
            "es_peligroso": True,
            "dimensiones_estimadas": "Aprox. 0.5 m3"
        },
        "archivo_procesado": foto.filename
    }

# ============================================================
# ENDPOINT — Health Check
# ============================================================

@app.get("/", tags=["Sistema"],
         summary="Health check")
async def health_check():
    """Verifica que la API esté funcionando."""
    return {
        "status": "OK",
        "app": "ECOLIM S.A.C. API",
        "version": "1.0.0",
        "total_registros": len(db_registros),
        "total_residuos": len(db_residuos),
        "sync_log_count": len(db_sync_log),
        "timestamp": datetime.now().isoformat()
    }


# ============================================================
# PUNTO DE ENTRADA
# ============================================================

if __name__ == "__main__":
    print("=" * 60)
    print("  ECOLIM S.A.C. — API de Residuos v1.0.0")
    print("  Swagger UI: http://localhost:8000/docs")
    print("  ReDoc:      http://localhost:8000/redoc")
    print("=" * 60)

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True
    )
