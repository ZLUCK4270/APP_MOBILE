package com.example.android.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.android.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ApiClient.java — Cliente HTTP para comunicación con la API REST Python.
 * Usa OkHttp3 para peticiones asíncronas con soporte JSON.
 * Todas las respuestas se entregan al UI thread vía Handler.
 */
public class ApiClient {

    private static final String TAG = "ECOLIM_API";

    /** MediaType para cuerpo JSON */
    private static final MediaType JSON_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    /** Instancia singleton del cliente OkHttp con timeout configurado */
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();

    /** Handler para ejecutar callbacks en el hilo principal (UI) */
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ============================================================
    // Interface de Callback
    // ============================================================

    /**
     * Interface de callback para operaciones asíncronas.
     */
    public interface ApiCallback {
        /** Se invoca cuando la API responde con HTTP 2xx */
        void onSuccess(String responseBody);
        /** Se invoca cuando hay error de red o HTTP 4xx/5xx */
        void onError(String errorMessage);
    }

    // ============================================================
    // SINCRONIZACIÓN — POST /api/v1/sync
    // ============================================================

    /**
     * Envía un lote de registros pendientes a la API para sincronización.
     *
     * @param registrosJson  JSONArray con los registros isSynced=0
     * @param dispositivoId  ID único del dispositivo Android
     * @param callback       Callback para manejar éxito/error en el UI thread
     */
    public static void sincronizarRegistros(JSONArray registrosJson,
                                             String dispositivoId,
                                             ApiCallback callback) {
        try {
            // Construir payload JSON
            JSONObject payload = new JSONObject();
            payload.put("dispositivo_id", dispositivoId);
            payload.put("registros", registrosJson);

            // Crear cuerpo de la petición
            RequestBody body = RequestBody.create(
                    payload.toString(), JSON_TYPE);

            // Construir petición POST
            Request request = new Request.Builder()
                    .url(Constants.API_BASE_URL + Constants.ENDPOINT_SYNC)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Device-ID", dispositivoId)
                    .build();

            Log.i(TAG, "Enviando POST " + Constants.ENDPOINT_SYNC
                    + " con " + registrosJson.length() + " registros...");

            // Ejecutar petición asíncrona
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Error de red: " + e.getMessage());
                    mainHandler.post(() ->
                            callback.onError("Error de red: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response)
                        throws IOException {
                    String responseBody = response.body() != null
                            ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        Log.i(TAG, "Sync exitoso: " + responseBody);
                        mainHandler.post(() -> callback.onSuccess(responseBody));
                    } else {
                        Log.e(TAG, "Error HTTP " + response.code()
                                + ": " + responseBody);
                        mainHandler.post(() ->
                                callback.onError("Error del servidor: "
                                        + response.code()));
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error al preparar payload: " + e.getMessage());
            callback.onError("Error interno: " + e.getMessage());
        }
    }

    // ============================================================
    // CATÁLOGOS — GET /api/v1/residuos
    // ============================================================

    /**
     * Obtiene la lista de residuos desde la API remota.
     */
    public static void obtenerResiduos(ApiCallback callback) {
        Request request = new Request.Builder()
                .url(Constants.API_BASE_URL + Constants.ENDPOINT_RESIDUOS)
                .get()
                .build();

        ejecutarGet(request, callback);
    }

    // ============================================================
    // REPORTES — GET /api/v1/reportes
    // ============================================================

    /**
     * Solicita un reporte al backend con filtros opcionales.
     */
    public static void obtenerReporte(String fechaInicio, String fechaFin,
                                       String formato, ApiCallback callback) {
        StringBuilder url = new StringBuilder(
                Constants.API_BASE_URL + Constants.ENDPOINT_REPORTES + "?");

        if (fechaInicio != null) url.append("fecha_inicio=").append(fechaInicio).append("&");
        if (fechaFin != null) url.append("fecha_fin=").append(fechaFin).append("&");
        if (formato != null) url.append("formato=").append(formato);

        Request request = new Request.Builder()
                .url(url.toString())
                .get()
                .build();

        ejecutarGet(request, callback);
    }

    // ============================================================
    // MÉTODO GENÉRICO PARA GET
    // ============================================================

    /**
     * Ejecuta una petición GET asíncrona genérica.
     */
    private static void ejecutarGet(Request request, ApiCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "GET Error: " + e.getMessage());
                mainHandler.post(() ->
                        callback.onError("Error de red: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {
                String responseBody = response.body() != null
                        ? response.body().string() : "";
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(responseBody));
                } else {
                    mainHandler.post(() ->
                            callback.onError("Error: " + response.code()));
                }
            }
        });
    }

    // ============================================================
    // AUTENTICACIÓN
    // ============================================================

    public static void registrarUsuario(JSONObject payload, ApiCallback callback) {
        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request request = new Request.Builder()
                .url(Constants.API_BASE_URL + "auth/register")
                .post(body)
                .build();
        
        ejecutarPost(request, callback);
    }

    public static void loginUsuario(JSONObject payload, ApiCallback callback) {
        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request request = new Request.Builder()
                .url(Constants.API_BASE_URL + "auth/login")
                .post(body)
                .build();
        
        ejecutarPost(request, callback);
    }

    public static void recuperarPassword(String correo, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("correo", correo);
            RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
            Request request = new Request.Builder()
                    .url(Constants.API_BASE_URL + "auth/forgot-password")
                    .post(body)
                    .build();
            ejecutarPost(request, callback);
        } catch (Exception e) {
            callback.onError("Error al preparar la solicitud: " + e.getMessage());
        }
    }

    public static void actualizarPerfil(int userId, JSONObject payload, ApiCallback callback) {
        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request request = new Request.Builder()
                .url(Constants.API_BASE_URL + "usuarios/perfil/" + userId)
                .put(body)
                .build();
        
        ejecutarPost(request, callback);
    }

    private static void ejecutarPost(Request request, ApiCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Error de red: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(responseBody));
                } else {
                    mainHandler.post(() -> callback.onError("Error: " + response.code() + " " + responseBody));
                }
            }
        });
    }
}
