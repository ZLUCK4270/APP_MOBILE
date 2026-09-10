package com.example.android.data.repository;

import android.app.Application;

import com.example.android.data.local.EcolimDatabase;
import com.example.android.data.local.dao.EcolimDao;
import com.example.android.data.local.entity.RecoleccionEntity;
import com.example.android.data.remote.EcolimApiService;
import com.example.android.data.remote.RetrofitClient;
import com.example.android.models.Reporte;
import com.example.android.utils.DateUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecoleccionRepository {

    private final EcolimDao dao;
    private final EcolimApiService apiService;
    private final ExecutorService executor;

    public RecoleccionRepository(Application application) {
        EcolimDatabase db = EcolimDatabase.getDatabase(application);
        this.dao = db.ecolimDao();
        this.apiService = RetrofitClient.getApiService();
        this.executor = EcolimDatabase.databaseWriteExecutor;
    }

    public void guardarRecoleccion(String clienteNombre, String residuoNombre,
                                   double peso, double volumen, String obs,
                                   RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            int idCliente = dao.getClienteIdPorNombre(clienteNombre);
            int idResiduo = dao.getResiduoIdPorNombre(residuoNombre);

            if (idCliente == 0 || idResiduo == 0) {
                callback.onResult(false);
                return;
            }

            RecoleccionEntity entity = new RecoleccionEntity();
            entity.idCliente = idCliente;
            entity.idResiduo = idResiduo;
            entity.peso = peso;
            entity.volumen = volumen;
            entity.observacion = obs;
            entity.fecha = DateUtils.fechaActualDB();
            entity.isSynced = 0;

            long result = dao.insertRecoleccion(entity);
            callback.onResult(result != -1);
        });
    }

    public void getDashboardStats(RepositoryCallback<DashboardStats> callback) {
        executor.execute(() -> {
            DashboardStats stats = new DashboardStats();
            stats.totalPeso = dao.getTotalPeso();
            stats.totalVolumen = dao.getTotalVolumen();
            stats.pendingCount = dao.getPendingCount();
            stats.totalRegistros = dao.getTotalRegistros();
            callback.onResult(stats);
        });
    }

    public void obtenerReportes(String fechaFiltro, String residuoFiltro, RepositoryCallback<List<Reporte>> callback) {
        executor.execute(() -> {
            List<Reporte> reportes = dao.obtenerReportes(fechaFiltro, residuoFiltro);
            callback.onResult(reportes);
        });
    }

    public void sincronizarConAPI(String dispositivoId, RepositoryCallback<SyncResult> callback) {
        executor.execute(() -> {
            List<RecoleccionEntity> pendientes = dao.getRegistrosPendientes();
            if (pendientes.isEmpty()) {
                callback.onResult(new SyncResult(true, 0, "No hay registros pendientes"));
                return;
            }

            JsonArray array = new JsonArray();
            List<Integer> ids = new ArrayList<>();
            for (RecoleccionEntity entity : pendientes) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id_registro", entity.idRecoleccion);
                obj.addProperty("id_usuario", 1); // Mock por ahora
                obj.addProperty("id_cliente", entity.idCliente);
                obj.addProperty("id_residuo", entity.idResiduo);
                obj.addProperty("peso_kg", entity.peso);
                obj.addProperty("volumen_m3", entity.volumen);
                obj.addProperty("observacion", entity.observacion);
                obj.addProperty("fecha_hora", entity.fecha);
                array.add(obj);
                ids.add(entity.idRecoleccion);
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("dispositivo_id", dispositivoId);
            payload.add("registros", array);

            apiService.syncRegistros(dispositivoId, payload).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        executor.execute(() -> {
                            dao.marcarComoSincronizados(ids);
                            callback.onResult(new SyncResult(true, ids.size(), "Sincronización exitosa"));
                        });
                    } else {
                        callback.onResult(new SyncResult(false, 0, "Error HTTP " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    callback.onResult(new SyncResult(false, 0, "Error de red: " + t.getMessage()));
                }
            });
        });
    }

    public interface RepositoryCallback<T> {
        void onResult(T data);
    }

    public static class DashboardStats {
        public double totalPeso;
        public double totalVolumen;
        public int pendingCount;
        public int totalRegistros;
    }

    public static class SyncResult {
        public boolean success;
        public int recordsSynced;
        public String message;
        public SyncResult(boolean success, int recordsSynced, String message) {
            this.success = success;
            this.recordsSynced = recordsSynced;
            this.message = message;
        }
    }
}
