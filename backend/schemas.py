from pydantic import BaseModel
from typing import List, Optional

# Schemas para Pydantic (Validación)
class RecoleccionSync(BaseModel):
    id_registro: int
    id_usuario: int
    id_cliente: int
    id_residuo: int
    peso_kg: float
    volumen_m3: float
    observacion: Optional[str] = None
    fecha_hora: str

class SyncPayload(BaseModel):
    dispositivo_id: str
    registros: List[RecoleccionSync]

class RegisterRequest(BaseModel):
    nombre: str
    apellido: str
    correo: str
    password: str

class LoginRequest(BaseModel):
    correo: str
    password: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str
    id_usuario: int
    usuario: str
    rol: str

class ForgotPasswordRequest(BaseModel):
    correo: str
