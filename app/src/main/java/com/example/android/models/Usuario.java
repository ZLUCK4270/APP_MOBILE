package com.example.android.models;

public class Usuario {
    private int idUsuario;
    private String nombreCompleto;
    private String correo;
    private String password;
    private String rolCargo;

    public Usuario() {}

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRolCargo() { return rolCargo; }
    public void setRolCargo(String rolCargo) { this.rolCargo = rolCargo; }
}
