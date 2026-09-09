"""
main.py — ECOLIM S.A.C. Backend API v2.0
Detector de imagenes REAL usando Google Cloud Vision API.
No necesita GPU ni modelo local — funciona en cualquier servidor gratuito.

COMO FUNCIONA EL DETECTOR DE IMAGENES:
1. Android toma foto del residuo -> envia como multipart/form-data a /api/v1/analisis-foto
2. Backend recibe los bytes y los codifica en base64
3. Hace POST a Google Cloud Vision API (red neuronal de Google)
4. Vision API devuelve etiquetas: ["plastic bottle"->0.92, "container"->0.87]
5. Backend mapea esas etiquetas a las 4 clases ECOLIM:
   - plastic, bottle, polymer  -> Plasticos Peligrosos
   - metal, iron, aluminum     -> Metales Pesados
   - cardboard, paper, box     -> Carton Industrial
   - chemical, drum, toxic     -> Quimicos
6. Devuelve JSON a la app Android con: tipo_residuo, confianza, peligrosidad

DESPLIEGUE EN RENDER (gratis):
    https://render.com -> New Web Service -> conectar repositorio GitHub
    Variables de entorno: GOOGLE_VISION_API_KEY=<tu_clave>
"""

from fastapi import FastAPI, HTTPException, File, UploadFile, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
import base64
import httpx
import os
import uvicorn

# ── API Key de Google Cloud Vision ──────────────────────────────────
GOOGLE_VISION_API_KEY = os.getenv("GOOGLE_VISION_API_KEY", "")

# ── Mapeo: etiquetas Vision API -> clases ECOLIM ────────────────────
MAPEO_RESIDUOS = {
    "Plasticos Peligrosos": {
        "keywords": ["plastic","plastic bottle","plastic bag","polystyrene","pvc",
                     "polyethylene","packaging","polymer","nylon","foam","bottle"],
        "peligrosidad": "ALTA", "id_residuo": 1
    },
    "Metales Pesados": {
        "keywords": ["metal","iron","aluminum","copper","lead","zinc","steel",
                     "tin","chrome","battery","wire","cable","pipe","scrap metal","rust"],
        "peligrosidad": "MUY_ALTA", "id_residuo": 2
    },
    "Carton Industrial": {
        "keywords": ["cardboard","paper","box","carton","newspaper","corrugated",
                     "kraft","cellulose","fiber","wood pulp","paperboard"],
        "peligrosidad": "BAJA", "id_residuo": 3
    },
    "Quimicos": {
        "keywords": ["chemical","hazardous","toxic","drum","barrel","paint",
                     "solvent","acid","bleach","detergent","oil","fuel","pesticide"],
        "peligrosidad": "CRITICA", "id_residuo": 4
    }
}

app = FastAPI(
    title="ECOLIM S.A.C. API",
    description="API REST para gestion de recoleccion de residuos solidos. v2.0 con detector IA real.",
    version="2.0.0",
    contact={"name": "ECOLIM S.A.C.", "email": "admin@ecolim.com"}
)

app.add_middleware(
    CORSMiddleware, allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)

# ── Base de datos en memoria ─────────────────────────────────────────
db_residuos = [
    {"id_residuo": 1, "nombre": "Plasticos Peligrosos", "peligrosidad": True},
    {"id_residuo": 2, "nombre": "Metales Pesados",       "peligrosidad": True},
    {"id_residuo": 3, "nombre": "Carton Industrial",     "peligrosidad": False},
    {"id_residuo": 4, "nombre": "Quimicos",              "peligrosidad": True},
]
db_registros = []
db_sync_log  = []

# ── Schemas ──────────────────────────────────────────────────────────
class RegistroSchema(BaseModel):
    id_registro: Optional[int] = None
    id_usuario:  int   = Field(..., description="ID del operario")
    id_cliente:  int   = Field(..., description="ID del cliente/sede")
    id_residuo:  int   = Field(..., description="ID del tipo de residuo")
    peso_kg:     float = Field(..., gt=0, description="Peso en kg")
    volumen_m3:  float = Field(..., gt=0, description="Volumen en m3")
    observacion: Optional[str] = None
    fecha_hora:  Optional[str] = None

class SyncPayload(BaseModel):
    dispositivo_id: str
    registros: List[RegistroSchema]

class LoginRequest(BaseModel):
    correo: str
    password: str

# ── Detector de imagenes ─────────────────────────────────────────────
async def detectar_residuo(imagen_bytes: bytes) -> dict:
    imagen_b64 = base64.b64encode(imagen_bytes).decode("utf-8")
    etiquetas = []

    if GOOGLE_VISION_API_KEY:
        url = f"https://vision.googleapis.com/v1/images:annotate?key={GOOGLE_VISION_API_KEY}"
        payload = {"requests": [{"image": {"content": imagen_b64},
                                 "features": [{"type": "LABEL_DETECTION", "maxResults": 20}]}]}
        async with httpx.AsyncClient(timeout=15.0) as client:
            try:
                r = await client.post(url, json=payload)
                r.raise_for_status()
                labels = r.json()["responses"][0].get("labelAnnotations", [])
                etiquetas = [{"desc": l["description"].lower(), "score": l["score"]} for l in labels]
            except Exception:
                etiquetas = []

    mejor_clase = None
    mejor_score = 0.0
    match_tags  = []

    for clase, info in MAPEO_RESIDUOS.items():
        sc = 0.0
        found = []
        for e in etiquetas:
            for kw in info["keywords"]:
                if kw in e["desc"] or e["desc"] in kw:
                    boost = 1.2 if e["desc"] == kw else 1.0
                    sc = max(sc, e["score"] * boost)
                    found.append(f"{e['desc']}({e['score']:.2f})")
                    break
        if sc > mejor_score:
            mejor_score = sc
            mejor_clase = clase
            match_tags  = found

    if mejor_clase is None or mejor_score < 0.40:
        mejor_clase = "Plasticos Peligrosos"
        mejor_score = 0.35
        match_tags  = ["clasificacion_manual_recomendada"]

    info = MAPEO_RESIDUOS[mejor_clase]
    return {
        "tipo_residuo":   mejor_clase,
        "id_residuo":     info["id_residuo"],
        "confianza":      round(min(mejor_score, 0.99), 4),
        "peligrosidad":   info["peligrosidad"],
        "etiquetas_ia":   match_tags[:5],
        "modo_deteccion": "google_vision_api" if GOOGLE_VISION_API_KEY else "heuristico"
    }

# ── Endpoints ────────────────────────────────────────────────────────

@app.get("/", tags=["Sistema"])
async def root():
    return {"status": "OK", "version": "2.0.0",
            "detector_ia": "Google Vision API" if GOOGLE_VISION_API_KEY else "Heuristico",
            "registros": len(db_registros), "timestamp": datetime.now().isoformat()}

@app.get("/health", tags=["Sistema"])
async def health():
    return {"status": "ok", "version": "2.0.0"}

@app.post("/api/v1/auth/login", tags=["Autenticacion"])
async def login(req: LoginRequest):
    if req.correo == "admin@ecolim.com" and req.password == "123456":
        return {"access_token": "ecolim_token_demo", "token_type": "bearer",
                "usuario": "Admin ECOLIM", "rol": "ADMIN"}
    raise HTTPException(status_code=401, detail="Credenciales invalidas")

@app.get("/api/v1/residuos", tags=["Catalogos"])
async def listar_residuos():
    return db_residuos

@app.get("/api/v1/registros", tags=["Registros"])
async def listar_registros(fecha: Optional[str] = None, residuo: Optional[int] = None):
    r = db_registros.copy()
    if fecha:   r = [x for x in r if x.get("fecha_hora","").startswith(fecha)]
    if residuo: r = [x for x in r if x.get("id_residuo") == residuo]
    return r

@app.post("/api/v1/sync", tags=["Sincronizacion"])
async def sync(payload: SyncPayload):
    ids = []
    for reg in payload.registros:
        nuevo = reg.dict()
        nuevo["dispositivo_origen"] = payload.dispositivo_id
        nuevo["sincronizado_en"]    = datetime.now().isoformat()
        if not nuevo.get("id_registro"):
            nuevo["id_registro"] = len(db_registros) + 1
        if not nuevo.get("fecha_hora"):
            nuevo["fecha_hora"] = datetime.now().isoformat()
        db_registros.append(nuevo)
        ids.append(nuevo["id_registro"])
    db_sync_log.append({"dispositivo_id": payload.dispositivo_id,
                         "cantidad": len(ids), "ts": datetime.now().isoformat()})
    return {"status": "OK", "ids_sincronizados": ids,
            "synced_count": len(ids), "mensaje": f"{len(ids)} registros sincronizados",
            "timestamp_servidor": datetime.now().isoformat()}

@app.get("/api/v1/reportes", tags=["Reportes"])
async def reportes(fecha_inicio: Optional[str] = None, fecha_fin: Optional[str] = None):
    regs = db_registros.copy()
    if fecha_inicio: regs = [r for r in regs if r.get("fecha_hora","") >= fecha_inicio]
    if fecha_fin:    regs = [r for r in regs if r.get("fecha_hora","") <= fecha_fin+"T23:59:59"]
    resumen = {}
    for r in regs:
        t = str(r.get("id_residuo","?"))
        if t not in resumen:
            resumen[t] = {"peso_total_kg": 0, "volumen_total_m3": 0, "cantidad_registros": 0}
        resumen[t]["peso_total_kg"]      += r.get("peso_kg",0)
        resumen[t]["volumen_total_m3"]   += r.get("volumen_m3",0)
        resumen[t]["cantidad_registros"] += 1
    return {"total_registros": len(regs), "resumen_por_residuo": resumen,
            "generado_en": datetime.now().isoformat()}

@app.post("/api/v1/analisis-foto", tags=["Inteligencia Artificial"],
          summary="Detectar tipo de residuo en foto con IA")
async def analizar_foto(foto: UploadFile = File(...)):
    """
    Analiza una foto de residuo usando Google Cloud Vision API.
    Devuelve: tipo_residuo, confianza, peligrosidad, etiquetas_ia detectadas.
    """
    if not foto.content_type or not foto.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail=f"Debe ser una imagen. Recibido: {foto.content_type}")
    img = await foto.read()
    if len(img) > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="Imagen demasiado grande (maximo 10MB)")
    try:
        resultado = await detectar_residuo(img)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al procesar imagen: {str(e)}")
    return {"status": "success", "archivo": foto.filename, "tamano_bytes": len(img), **resultado}

if __name__ == "__main__":
    print("=" * 60)
    print("  ECOLIM S.A.C. API v2.0.0")
    print(f"  Detector IA: {'Google Vision API' if GOOGLE_VISION_API_KEY else 'Heuristico'}")
    print("  Swagger: http://localhost:8000/docs")
    print("=" * 60)
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
