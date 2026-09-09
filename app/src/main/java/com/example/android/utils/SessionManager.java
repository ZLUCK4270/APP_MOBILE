package com.example.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager.java — Manejo de sesión del usuario con SharedPreferences.
 * Persiste los datos del usuario logueado para evitar login repetitivo
 * y para mostrar información personalizada en el Dashboard.
 */
public class SessionManager {

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        SharedPreferences securePrefs = null;
        try {
            androidx.security.crypto.MasterKey masterKey = new androidx.security.crypto.MasterKey.Builder(context)
                    .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                    context,
                    Constants.PREFS_NAME,
                    masterKey,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback en caso de error crítico con Keystore (poco probable, pero manejado)
            e.printStackTrace();
            securePrefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        }
        prefs = securePrefs;
        editor = prefs.edit();
    }

    // ============================================================
    // Métodos de Login / Logout
    // ============================================================

    /**
     * Guarda los datos de la sesión tras un login exitoso.
     * @param userId    ID del usuario en la tabla Usuario
     * @param nombre    Nombre completo del operario
     * @param correo    Correo corporativo
     * @param rol       Rol/cargo (OPERARIO, SUPERVISOR, ADMIN)
     */
    public void crearSesion(int userId, String nombre, String correo, String rol) {
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);
        editor.putInt(Constants.PREF_USER_ID, userId);
        editor.putString(Constants.PREF_USER_NAME, nombre);
        editor.putString(Constants.PREF_USER_EMAIL, correo);
        editor.putString(Constants.PREF_USER_ROL, rol);
        editor.apply(); // apply() es asíncrono (más rápido que commit())
    }

    /**
     * Elimina todos los datos de sesión (logout).
     */
    public void cerrarSesion() {
        editor.clear();
        editor.apply();
    }

    // ============================================================
    // Getters de sesión
    // ============================================================

    /** @return true si hay un usuario con sesión activa */
    public boolean isLoggedIn() {
        return prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    /** @return ID del usuario logueado, o -1 si no hay sesión */
    public int getUserId() {
        return prefs.getInt(Constants.PREF_USER_ID, -1);
    }

    /** @return Nombre completo del usuario logueado */
    public String getUserName() {
        return prefs.getString(Constants.PREF_USER_NAME, "Operario");
    }

    /** @return Correo del usuario logueado */
    public String getUserEmail() {
        return prefs.getString(Constants.PREF_USER_EMAIL, "");
    }

    /** @return Rol/cargo del usuario logueado */
    public String getUserRol() {
        return prefs.getString(Constants.PREF_USER_ROL, "OPERARIO");
    }
}
