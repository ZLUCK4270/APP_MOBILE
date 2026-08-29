package com.example.android.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DateUtils.java — Utilidades de formateo de fechas para la app ECOLIM.
 * Centraliza formatos para evitar inconsistencias entre pantallas.
 */
public class DateUtils {

    /** Formato para almacenar en SQLite (ISO 8601) */
    private static final SimpleDateFormat FORMAT_DB =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    /** Formato para mostrar en la UI (día/mes/año) */
    private static final SimpleDateFormat FORMAT_UI =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    /** Formato completo con hora para reportes */
    private static final SimpleDateFormat FORMAT_FULL =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    /** Formato ISO 8601 completo para API */
    private static final SimpleDateFormat FORMAT_ISO =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    /**
     * Retorna la fecha actual en formato YYYY-MM-DD (para SQLite).
     */
    public static String fechaActualDB() {
        return FORMAT_DB.format(new Date());
    }

    /**
     * Retorna la fecha actual en formato DD/MM/YYYY (para UI).
     */
    public static String fechaActualUI() {
        return FORMAT_UI.format(new Date());
    }

    /**
     * Retorna la fecha y hora actual en formato completo (para reportes).
     */
    public static String fechaHoraActual() {
        return FORMAT_FULL.format(new Date());
    }

    /**
     * Retorna la fecha y hora actual en formato ISO 8601 (para API).
     */
    public static String fechaISO() {
        return FORMAT_ISO.format(new Date());
    }

    /**
     * Convierte una fecha de formato DB (YYYY-MM-DD) a formato UI (DD/MM/YYYY).
     */
    public static String dbToUI(String fechaDB) {
        try {
            Date date = FORMAT_DB.parse(fechaDB);
            return date != null ? FORMAT_UI.format(date) : fechaDB;
        } catch (Exception e) {
            return fechaDB; // Retornar original si no se puede parsear
        }
    }
}
