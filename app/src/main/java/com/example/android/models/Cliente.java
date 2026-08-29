package com.example.android.models;

/**
 * Cliente.java — Modelo de cliente/sede.
 * Mapea la tabla 'Cliente' de SQLite.
 * Incluye campos extendidos para RUC, sede y dirección.
 */
public class Cliente {
    private int idCliente;
    private String razonSocial;
    private String ruc;
    private String sedePlanta;
    private String direccion;

    public Cliente() {}

    public Cliente(int idCliente, String razonSocial) {
        this.idCliente = idCliente;
        this.razonSocial = razonSocial;
    }

    public Cliente(int idCliente, String razonSocial, String ruc,
                   String sedePlanta, String direccion) {
        this.idCliente = idCliente;
        this.razonSocial = razonSocial;
        this.ruc = ruc;
        this.sedePlanta = sedePlanta;
        this.direccion = direccion;
    }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getSedePlanta() { return sedePlanta; }
    public void setSedePlanta(String sedePlanta) { this.sedePlanta = sedePlanta; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}
