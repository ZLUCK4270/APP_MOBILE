package com.example.android.data.repository;

import android.app.Application;

import com.example.android.data.local.EcolimDatabase;
import com.example.android.data.local.dao.EcolimDao;
import com.example.android.data.remote.EcolimApiService;
import com.example.android.data.remote.RetrofitClient;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class CatalogoRepository {
    private final EcolimDao dao;
    private final EcolimApiService apiService;
    private final ExecutorService executor;

    public CatalogoRepository(Application application) {
        EcolimDatabase db = EcolimDatabase.getDatabase(application);
        this.dao = db.ecolimDao();
        this.apiService = RetrofitClient.getApiService();
        this.executor = EcolimDatabase.databaseWriteExecutor;
    }

    public void obtenerClientes(RepositoryCallback<List<String>> callback) {
        executor.execute(() -> {
            List<String> clientes = dao.getNombresClientes();
            callback.onResult(clientes);
        });
    }

    public void obtenerResiduos(RepositoryCallback<List<String>> callback) {
        executor.execute(() -> {
            List<String> residuos = dao.getNombresResiduos();
            callback.onResult(residuos);
        });
    }

    public interface RepositoryCallback<T> {
        void onResult(T data);
    }
}
