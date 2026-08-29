package com.example.android.models;

/**
 * DetalleRecoleccion.java — Modelo de detalle de una recolección.
 * Mapea la tabla 'Detalle_recoleccion' de SQLite.
 * Cada detalle pertenece a una Recoleccion (cabecera) y referencia un Residuo.
 */
public class DetalleRecoleccion {
    private int idDetalle;
    private int idRecoleccion;   // FK → Recoleccion (cabecera)
    private int idResiduo;       // FK → Residuo (tipo)
    private double pesoKg;
    private double volumenM3;
    private String observacion;

    public DetalleRecoleccion() {}

    public DetalleRecoleccion(int idDetalle, int idRecoleccion, int idResiduo,
                               double pesoKg, double volumenM3, String observacion) {
        this.idDetalle = idDetalle;
        this.idRecoleccion = idRecoleccion;
        this.idResiduo = idResiduo;
        this.pesoKg = pesoKg;
        this.volumenM3 = volumenM3;
        this.observacion = observacion;
    }

    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }

    public int getIdRecoleccion() { return idRecoleccion; }
    public void setIdRecoleccion(int idRecoleccion) { this.idRecoleccion = idRecoleccion; }

    public int getIdResiduo() { return idResiduo; }
    public void setIdResiduo(int idResiduo) { this.idResiduo = idResiduo; }

    public double getPesoKg() { return pesoKg; }
    public void setPesoKg(double pesoKg) { this.pesoKg = pesoKg; }

    public double getVolumenM3() { return volumenM3; }
    public void setVolumenM3(double volumenM3) { this.volumenM3 = volumenM3; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
