package com.example.android.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "Recoleccion",
    foreignKeys = {
        @ForeignKey(entity = ClienteEntity.class,
                    parentColumns = "idCliente",
                    childColumns = "idCliente",
                    onDelete = ForeignKey.RESTRICT),
        @ForeignKey(entity = ResiduoEntity.class,
                    parentColumns = "idResiduo",
                    childColumns = "idResiduo",
                    onDelete = ForeignKey.RESTRICT)
    }
)
public class RecoleccionEntity {
    @PrimaryKey(autoGenerate = true)
    public int idRecoleccion;

    public int idCliente;
    public int idResiduo;
    
    public double peso;
    public double volumen;
    public String observacion;
    public String fecha;
    public int isSynced; // 0 = pendiente, 1 = sincronizado
}
