package com.example.android.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Cliente")
public class ClienteEntity {
    @PrimaryKey(autoGenerate = true)
    public int idCliente;

    public String razonSocial;
}
