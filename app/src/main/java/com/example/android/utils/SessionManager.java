package com.example.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.File;

/**
 * SessionManager.java — Manejo de sesión del usuario con SharedPreferences.
 * Persiste los datos del usuario logueado.
 */
public class SessionManager {

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final Context mContext;

    public SessionManager(Context context) {
        this.mContext = context.getApplicationContext();
        SharedPreferences securePrefs = null;
        
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        boolean isXiaomi = manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco");

        if (isXiaomi) {
            // Fallback directo para Xiaomi/MIUI debido al bug de Keystore (SecurityException)
            Log.w("SessionManager", "Dispositivo Xiaomi detectado. Usando SharedPreferences estándar.");
            securePrefs = mContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        } else {
            try {
                MasterKey masterKey = new MasterKey.Builder(mContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                securePrefs = EncryptedSharedPreferences.create(
                        mContext,
                        Constants.PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (Exception e) {
                // Si el Keystore está corrupto en otros dispositivos, limpiamos el archivo roto y usamos el estándar
                e.printStackTrace();
                clearCorruptedPrefs(mContext);
                securePrefs = mContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            }
        }
        
        prefs = securePrefs;
        editor = prefs.edit();
    }
    
    private void clearCorruptedPrefs(Context context) {
        try {
            File dir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
            File prefsFile = new File(dir, Constants.PREFS_NAME + ".xml");
            if (prefsFile.exists()) {
                prefsFile.delete();
            }
        } catch (Exception ignored) {}
    }

    public void crearSesion(int userId, String nombre, String correo, String rol) {
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);
        editor.putInt(Constants.PREF_USER_ID, userId);
        editor.putString(Constants.PREF_USER_NAME, nombre);
        editor.putString(Constants.PREF_USER_EMAIL, correo);
        editor.putString(Constants.PREF_USER_ROL, rol);
        editor.apply();
    }

    public void cerrarSesion() {
        editor.clear();
        editor.apply();
    }

    public boolean isLoggedIn() {
        try {
            return prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
        } catch (Exception e) {
            // Si ocurre un error de desencriptación en runtime
            cerrarSesion();
            return false;
        }
    }

    public int getUserId() {
        try {
            return prefs.getInt(Constants.PREF_USER_ID, -1);
        } catch (Exception e) { return -1; }
    }

    public String getUserName() {
        try {
            return prefs.getString(Constants.PREF_USER_NAME, "Operario");
        } catch (Exception e) { return "Operario"; }
    }

    public String getUserEmail() {
        try {
            return prefs.getString(Constants.PREF_USER_EMAIL, "");
        } catch (Exception e) { return ""; }
    }

    public String getUserRol() {
        try {
            return prefs.getString(Constants.PREF_USER_ROL, "OPERARIO");
        } catch (Exception e) { return "OPERARIO"; }
    }
}
