package com.example.android.utils;

/**
 * Constants.java — Constantes globales de la aplicación ECOLIM.
 * Centraliza URLs, claves de SharedPreferences y configuraciones.
 */
public class Constants {

    // ============================================================
    // URLs de la API REST (Backend Python - FastAPI)
    // ============================================================

    /** URL base para emulador Android (10.0.2.2 = localhost del host) */
    public static final String API_BASE_URL_EMULATOR = "http://10.0.2.2:8000/api/v1";

    /** URL base para dispositivo físico en red local */
    public static final String API_BASE_URL_LOCAL = "http://192.168.1.100:8000/api/v1";

    /** URL activa — cambiar según entorno de pruebas */
    public static final String API_BASE_URL = API_BASE_URL_EMULATOR;

    // ============================================================
    // Endpoints de la API
    // ============================================================

    public static final String ENDPOINT_SYNC = "/sync";
    public static final String ENDPOINT_RESIDUOS = "/residuos";
    public static final String ENDPOINT_REGISTROS = "/registros";
    public static final String ENDPOINT_REPORTES = "/reportes";

    // ============================================================
    // SharedPreferences
    // ============================================================

    /** Nombre del archivo de SharedPreferences */
    public static final String PREFS_NAME = "ecolim_prefs";

    /** Claves para datos de sesión del usuario */
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_ROL = "user_rol";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";

    // ============================================================
    // Configuración de la Base de Datos SQLite
    // ============================================================

    public static final String DATABASE_NAME = "ecolim_db.db";
    public static final int DATABASE_VERSION = 2;

    // ============================================================
    // Códigos QR — Formato esperado
    // ============================================================

    /** Prefijo de los códigos QR de ECOLIM: "ECOLIM|{idResiduo}|{nombre}" */
    public static final String QR_PREFIX = "ECOLIM";
    public static final String QR_SEPARATOR = "\\|";

    // ============================================================
    // Configuración de red
    // ============================================================

    public static final int HTTP_TIMEOUT_SECONDS = 30;
    public static final int SYNC_RETRY_MAX = 3;

    // ============================================================
    // Validaciones de formulario
    // ============================================================

    public static final double PESO_MIN = 0.1;
    public static final double PESO_MAX = 50000.0;
    public static final double VOLUMEN_MIN = 0.01;
    public static final double VOLUMEN_MAX = 1000.0;
    public static final double DENSIDAD_MIN = 10.0;
    public static final double DENSIDAD_MAX = 5000.0;
}
