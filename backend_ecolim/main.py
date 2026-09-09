"""
main.py — ECOLIM S.A.C. Backend API v3.0
Incluye Base de Datos real con SQLAlchemy (SQLite/PostgreSQL) 
y autenticación completa con registro de usuarios y perfil.
"""

from fastapi import FastAPI, HTTPException, File, UploadFile, status, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, EmailStr
from typing import List, Optional
from datetime import datetime
import base64
import httpx
import os
import uvicorn
from passlib.context import CryptContext

# --- SQLAlchemy ---
from sqlalchemy import create_engine, Column, Integer, String, Float, Boolean, DateTime
from sqlalchemy.orm import declarative_base
from sqlalchemy.orm import sessionmaker, Session

# ============================================================
# CONFIGURACIÓN DE BASE DE DATOS Y SEGURIDAD
# ============================================================

# Usar SQLite local por defecto. 
# En Render, configurar DATABASE_URL con la URL de PostgreSQL.
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./ecolim.db")

# Para SQLite necesitamos un argumento especial, para Postgres no.
connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}
engine = create_engine(DATABASE_URL, connect_args=connect_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# Hashing de contraseñas
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# ============================================================
# MODELOS DE BASE DE DATOS (ORM)
# ============================================================

class UsuarioDB(Base):
    __tablename__ = "usuarios"
    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, index=True)
    apellido = Column(String)
    correo = Column(String, unique=True, index=True)
    password_hash = Column(String)
    rol = Column(String, default="OPERARIO")
    creado_en = Column(DateTime, default=datetime.utcnow)

class RegistroDB(Base):
    __tablename__ = "registros"
    id_registro = Column(Integer, primary_key=True, index=True)
    id_usuario = Column(Integer)
    id_cliente = Column(Integer)
    id_residuo = Column(Integer)
    peso_kg = Column(Float)
    volumen_m3 = Column(Float)
    observacion = Column(String)
    fecha_hora = Column(String)
    dispositivo_origen = Column(String)
    sincronizado_en = Column(String)

# Crear las tablas
Base.metadata.create_all(bind=engine)

# ============================================================
# CONFIGURACIÓN DE APP Y DETECTOR DE IMÁGENES
# ============================================================

GOOGLE_VISION_API_KEY = os.getenv("GOOGLE_VISION_API_KEY", "")

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

app = FastAPI(title="ECOLIM S.A.C. API v3.0", version="3.0.0")

app.add_middleware(
    CORSMiddleware, allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)

# Catálogo estático para simplificar la demo
db_residuos = [
    {"id_residuo": 1, "nombre": "Plasticos Peligrosos", "peligrosidad": True},
    {"id_residuo": 2, "nombre": "Metales Pesados",       "peligrosidad": True},
    {"id_residuo": 3, "nombre": "Carton Industrial",     "peligrosidad": False},
    {"id_residuo": 4, "nombre": "Quimicos",              "peligrosidad": True},
]

# ============================================================
# SCHEMAS PYDANTIC (Para Validacion)
# ============================================================

class UsuarioRegister(BaseModel):
    nombre: str
    apellido: str
    correo: EmailStr
    password: str

class UsuarioUpdate(BaseModel):
    nombre: Optional[str] = None
    apellido: Optional[str] = None
    correo: Optional[EmailStr] = None

class UsuarioResponse(BaseModel):
    id: int
    nombre: str
    apellido: str
    correo: str
    rol: str

    class Config:
        from_attributes = True

class LoginRequest(BaseModel):
    correo: str
    password: str

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


# ============================================================
# ENDPOINTS DE AUTENTICACIÓN Y USUARIOS
# ============================================================

@app.post("/api/v1/auth/register", response_model=UsuarioResponse, tags=["Autenticacion"])
async def register(user: UsuarioRegister, db: Session = Depends(get_db)):
    """Registra un nuevo usuario en la base de datos."""
    # Verificar si el correo ya existe
    db_user = db.query(UsuarioDB).filter(UsuarioDB.correo == user.correo).first()
    if db_user:
        raise HTTPException(status_code=400, detail="El correo ya está registrado")
    
    # Hashear contraseña
    hashed_password = pwd_context.hash(user.password)
    
    # Crear usuario
    nuevo_usuario = UsuarioDB(
        nombre=user.nombre,
        apellido=user.apellido,
        correo=user.correo,
        password_hash=hashed_password,
        rol="OPERARIO"
    )
    db.add(nuevo_usuario)
    db.commit()
    db.refresh(nuevo_usuario)
    return nuevo_usuario

@app.post("/api/v1/auth/login", tags=["Autenticacion"])
async def login(req: LoginRequest, db: Session = Depends(get_db)):
    """Inicia sesión verificando credenciales en la base de datos."""
    # Soporte para la cuenta quemada de demo por si la BD local está vacía
    if req.correo == "admin@ecolim.com" and req.password == "123456":
        return {"access_token": "ecolim_token_demo", "token_type": "bearer",
                "id_usuario": 0, "usuario": "Admin ECOLIM", "correo": req.correo, "rol": "ADMIN"}
    
    user = db.query(UsuarioDB).filter(UsuarioDB.correo == req.correo).first()
    if not user or not pwd_context.verify(req.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Credenciales invalidas")
    
    # En un entorno real se usaría JWT. Aquí simulamos el token para el MVP.
    return {
        "access_token": f"token_user_{user.id}", 
        "token_type": "bearer",
        "id_usuario": user.id,
        "usuario": f"{user.nombre} {user.apellido}",
        "correo": user.correo,
        "rol": user.rol
    }

@app.get("/api/v1/usuarios/perfil/{user_id}", response_model=UsuarioResponse, tags=["Usuarios"])
async def get_profile(user_id: int, db: Session = Depends(get_db)):
    """Obtiene los datos del perfil de un usuario."""
    user = db.query(UsuarioDB).filter(UsuarioDB.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    return user

@app.put("/api/v1/usuarios/perfil/{user_id}", response_model=UsuarioResponse, tags=["Usuarios"])
async def update_profile(user_id: int, update_data: UsuarioUpdate, db: Session = Depends(get_db)):
    """Actualiza el nombre, apellido o correo de un usuario."""
    user = db.query(UsuarioDB).filter(UsuarioDB.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    
    if update_data.nombre:
        user.nombre = update_data.nombre
    if update_data.apellido:
        user.apellido = update_data.apellido
    if update_data.correo:
        # Verificar que el nuevo correo no esté tomado
        correo_existente = db.query(UsuarioDB).filter(UsuarioDB.correo == update_data.correo, UsuarioDB.id != user_id).first()
        if correo_existente:
            raise HTTPException(status_code=400, detail="El correo ya está en uso por otro usuario")
        user.correo = update_data.correo

    db.commit()
    db.refresh(user)
    return user


# ============================================================
# ENDPOINTS RESTANTES (Residuos, Sync, Reportes, Foto)
# ============================================================

@app.get("/api/v1/residuos", tags=["Catalogos"])
async def listar_residuos():
    return db_residuos

@app.get("/api/v1/registros", tags=["Registros"])
async def listar_registros(fecha: Optional[str] = None, residuo: Optional[int] = None, db: Session = Depends(get_db)):
    query = db.query(RegistroDB)
    if fecha:
        query = query.filter(RegistroDB.fecha_hora.startswith(fecha))
    if residuo:
        query = query.filter(RegistroDB.id_residuo == residuo)
    return query.all()

@app.post("/api/v1/sync", tags=["Sincronizacion"])
async def sync(payload: SyncPayload, db: Session = Depends(get_db)):
    ids = []
    for reg in payload.registros:
        nuevo_registro = RegistroDB(
            id_usuario=reg.id_usuario,
            id_cliente=reg.id_cliente,
            id_residuo=reg.id_residuo,
            peso_kg=reg.peso_kg,
            volumen_m3=reg.volumen_m3,
            observacion=reg.observacion,
            fecha_hora=reg.fecha_hora or datetime.now().isoformat(),
            dispositivo_origen=payload.dispositivo_id,
            sincronizado_en=datetime.now().isoformat()
        )
        db.add(nuevo_registro)
        db.commit()
        db.refresh(nuevo_registro)
        ids.append(nuevo_registro.id_registro)
        
    return {"status": "OK", "ids_sincronizados": ids,
            "synced_count": len(ids), "mensaje": f"{len(ids)} registros sincronizados",
            "timestamp_servidor": datetime.now().isoformat()}

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
    for clase, info in MAPEO_RESIDUOS.items():
        sc = 0.0
        for e in etiquetas:
            for kw in info["keywords"]:
                if kw in e["desc"] or e["desc"] in kw:
                    sc = max(sc, e["score"] * (1.2 if e["desc"] == kw else 1.0))
                    break
        if sc > mejor_score:
            mejor_score = sc
            mejor_clase = clase

    if mejor_clase is None or mejor_score < 0.40:
        mejor_clase = "Plasticos Peligrosos"
        mejor_score = 0.35

    info = MAPEO_RESIDUOS[mejor_clase]
    return {
        "tipo_residuo":   mejor_clase,
        "id_residuo":     info["id_residuo"],
        "confianza":      round(min(mejor_score, 0.99), 4),
        "peligrosidad":   info["peligrosidad"],
    }

@app.post("/api/v1/analisis-foto", tags=["Inteligencia Artificial"])
async def analizar_foto(foto: UploadFile = File(...)):
    img = await foto.read()
    try:
        resultado = await detectar_residuo(img)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    return {"status": "success", "archivo": foto.filename, **resultado}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
