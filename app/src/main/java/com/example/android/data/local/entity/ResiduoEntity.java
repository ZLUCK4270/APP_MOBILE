package com.example.android.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Residuo")
public class ResiduoEntity {
    @PrimaryKey(autoGenerate = true)
    public int idResiduo;

    public String nombre;
}
