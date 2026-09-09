from fastapi import FastAPI, Depends, HTTPException, status
from sqlalchemy.orm import Session
import models, schemas, database
from typing import List

# Crear tablas (en producción usar Alembic)
models.Base.metadata.create_all(bind=database.engine)

app = FastAPI(title="ECOLIM API", version="1.0.0")

@app.post("/api/v1/auth/login", response_model=schemas.TokenResponse)
def login(request: schemas.LoginRequest, db: Session = Depends(database.get_db)):
    # Lógica simplificada. En producción: Hash de password y JWT real.
    user = db.query(models.Usuario).filter(models.Usuario.correo == request.correo).first()
    if not user or request.password != "123456": # Dummy check por MVP
        raise HTTPException(status_code=401, detail="Credenciales inválidas")
    
    return {"access_token": "dummy_jwt_token_for_" + str(user.id), "token_type": "bearer"}

@app.post("/api/v1/sync")
def sync_records(payload: schemas.SyncPayload, db: Session = Depends(database.get_db)):
    # Endpoint para recibir registros offline
    registros_guardados = 0
    for reg in payload.registros:
        # Aquí se validarían reglas de negocio también en backend
        db_rec = models.Recoleccion(
            id_usuario=reg.id_usuario,
            id_cliente=reg.id_cliente,
            id_residuo=reg.id_residuo,
            peso=reg.peso_kg,
            volumen=reg.volumen_m3,
            observacion=reg.observacion,
            fecha=reg.fecha_hora
        )
        db.add(db_rec)
        registros_guardados += 1
    
    db.commit()
    return {"status": "success", "synced_count": registros_guardados, "message": "Registros sincronizados correctamente."}

@app.get("/api/v1/residuos")
def get_residuos(db: Session = Depends(database.get_db)):
    return db.query(models.Residuo).all()
