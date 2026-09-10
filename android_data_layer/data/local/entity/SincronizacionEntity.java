package com.example.android.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "Sincronizacion",
    foreignKeys = @ForeignKey(entity = RecoleccionEntity.class,
                              parentColumns = "idRecoleccion",
                              childColumns = "idRecoleccion",
                              onDelete = ForeignKey.CASCADE)
)
public class SincronizacionEntity {
    @PrimaryKey(autoGenerate = true)
    public int idSync;

    public int idRecoleccion;
    public String fechaIntento;
    public String estadoSync;
    public String respuestaApi;
}
