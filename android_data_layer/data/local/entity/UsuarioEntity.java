package com.example.android.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Usuario")
public class UsuarioEntity {
    @PrimaryKey(autoGenerate = true)
    public int idUsuario;

    public String nombre;
    public String correo;
    public String password;
    public String rol;
}
