from sqlalchemy import Column, Integer, String, Float, ForeignKey, DateTime
from database import Base
import datetime

class Usuario(Base):
    __tablename__ = "usuarios"
    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String)
    correo = Column(String, unique=True, index=True)
    password_hash = Column(String)
    rol = Column(String, default="OPERARIO")

class Cliente(Base):
    __tablename__ = "clientes"
    id = Column(Integer, primary_key=True, index=True)
    razon_social = Column(String, index=True)

class Residuo(Base):
    __tablename__ = "residuos"
    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, index=True)

class Recoleccion(Base):
    __tablename__ = "recolecciones"
    id = Column(Integer, primary_key=True, index=True)
    id_usuario = Column(Integer, ForeignKey("usuarios.id"))
    id_cliente = Column(Integer, ForeignKey("clientes.id"))
    id_residuo = Column(Integer, ForeignKey("residuos.id"))
    peso = Column(Float)
    volumen = Column(Float)
    observacion = Column(String, nullable=True)
    fecha = Column(String)  # Para simplificar con SQLite/Android
    sincronizado_en = Column(DateTime, default=datetime.datetime.utcnow)
