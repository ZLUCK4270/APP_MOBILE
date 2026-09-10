package com.example.android.models;

public class Reporte {
    private int idRecoleccion;
    private String cliente;
    private String residuo;
    private double peso;
    private double volumen;
    private String fecha;
    private boolean isSynced;

    public Reporte(int idRecoleccion, String cliente, String residuo, double peso, double volumen, String fecha, boolean isSynced) {
        this.idRecoleccion = idRecoleccion;
        this.cliente = cliente;
        this.residuo = residuo;
        this.peso = peso;
        this.volumen = volumen;
        this.fecha = fecha;
        this.isSynced = isSynced;
    }

    public int getIdRecoleccion() { return idRecoleccion; }
    public String getCliente() { return cliente; }
    public String getResiduo() { return residuo; }
    public double getPeso() { return peso; }
    public double getVolumen() { return volumen; }
    public String getFecha() { return fecha; }
    public boolean isSynced() { return isSynced; }
}
