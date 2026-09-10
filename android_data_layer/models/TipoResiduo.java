package com.example.android.models;

/**
 * TipoResiduo.java — Modelo de subclasificación de residuo.
 * Mapea la tabla 'Tipo_Residuo' de SQLite.
 * Cada TipoResiduo pertenece a un Residuo padre y tiene un código de color NTP.
 */
public class TipoResiduo {
    private int idTipo;
    private int idResiduo;       // FK → Residuo padre
    private String codigoColor;  // Código de color NTP (ROJO, AMARILLO, NEGRO, etc.)

    public TipoResiduo() {}

    public TipoResiduo(int idTipo, int idResiduo, String codigoColor) {
        this.idTipo = idTipo;
        this.idResiduo = idResiduo;
        this.codigoColor = codigoColor;
    }

    public int getIdTipo() { return idTipo; }
    public void setIdTipo(int idTipo) { this.idTipo = idTipo; }

    public int getIdResiduo() { return idResiduo; }
    public void setIdResiduo(int idResiduo) { this.idResiduo = idResiduo; }

    public String getCodigoColor() { return codigoColor; }
    public void setCodigoColor(String codigoColor) { this.codigoColor = codigoColor; }
}
