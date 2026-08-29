package com.example.android.api;

/**
 * EcolimApiService.java — Interfaz de servicio de la API ECOLIM.
 * Define los contratos de los endpoints REST disponibles.
 *
 * En una implementación con Retrofit, esta interfaz usaría anotaciones
 * como @GET, @POST, etc. En la implementación actual con OkHttp puro,
 * sirve como documentación y contrato de la API.
 */
public interface EcolimApiService {

    // ============================================================
    // Endpoints disponibles en la API Python (FastAPI)
    // ============================================================

    /**
     * POST /api/v1/sync
     * Sincroniza un lote de registros pendientes desde el dispositivo.
     *
     * Request Body:
     * {
     *   "dispositivo_id": "android-xxxx",
     *   "registros": [
     *     {
     *       "id_registro": 1,
     *       "id_usuario": 1,
     *       "id_cliente": 2,
     *       "id_residuo": 3,
     *       "peso_kg": 45.5,
     *       "volumen_m3": 1.2,
     *       "observacion": "Sin novedad",
     *       "fecha_hora": "2026-08-28T10:30:00"
     *     }
     *   ]
     * }
     *
     * Response 200:
     * {
     *   "status": "OK",
     *   "mensaje": "5 registros sincronizados",
     *   "ids_sincronizados": [1, 2, 3, 4, 5],
     *   "timestamp_servidor": "2026-08-28T10:30:05"
     * }
     */
    String SYNC = "/sync";

    /**
     * GET /api/v1/residuos
     * Retorna el catálogo de tipos de residuo.
     *
     * Response 200:
     * [
     *   { "id_residuo": 1, "nombre": "Plásticos Peligrosos", "peligrosidad": true },
     *   { "id_residuo": 2, "nombre": "Cartón Industrial", "peligrosidad": false }
     * ]
     */
    String RESIDUOS = "/residuos";

    /**
     * GET /api/v1/registros?fecha=2026-08-28&residuo=1
     * Lista registros de recolección con filtros opcionales.
     */
    String REGISTROS = "/registros";

    /**
     * GET /api/v1/reportes?fecha_inicio=2026-08-01&fecha_fin=2026-08-28&formato=json
     * Genera un reporte consolidado por periodo.
     */
    String REPORTES = "/reportes";
}
