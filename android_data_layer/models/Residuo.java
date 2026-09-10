package com.example.android.models;

/**
 * Residuo.java — Modelo de tipo de residuo.
 * Mapea la tabla 'Residuo' de SQLite.
 * Incluye campo de peligrosidad según NTP 900.058-2024.
 */
public class Residuo {
    private int idResiduo;
    private String nombreResiduo;
    private boolean peligrosidad; // true = Peligroso, false = No Peligroso

    public Residuo() {}

    public Residuo(int idResiduo, String nombreResiduo) {
        this.idResiduo = idResiduo;
        this.nombreResiduo = nombreResiduo;
        this.peligrosidad = false;
    }

    public Residuo(int idResiduo, String nombreResiduo, boolean peligrosidad) {
        this.idResiduo = idResiduo;
        this.nombreResiduo = nombreResiduo;
        this.peligrosidad = peligrosidad;
    }

    public int getIdResiduo() { return idResiduo; }
    public void setIdResiduo(int idResiduo) { this.idResiduo = idResiduo; }

    public String getNombreResiduo() { return nombreResiduo; }
    public void setNombreResiduo(String nombreResiduo) { this.nombreResiduo = nombreResiduo; }

    public boolean isPeligrosidad() { return peligrosidad; }
    public void setPeligrosidad(boolean peligrosidad) { this.peligrosidad = peligrosidad; }
}
