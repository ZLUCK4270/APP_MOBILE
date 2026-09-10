package com.example.android.ui.recoleccion;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.android.data.repository.CatalogoRepository;
import com.example.android.data.repository.RecoleccionRepository;

import java.util.List;

public class RecoleccionViewModel extends AndroidViewModel {

    private final CatalogoRepository catalogoRepository;
    private final RecoleccionRepository recoleccionRepository;

    private final MutableLiveData<List<String>> clientesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> residuosLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSavingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saveSuccessLiveData = new MutableLiveData<>();

    public RecoleccionViewModel(@NonNull Application application) {
        super(application);
        catalogoRepository = new CatalogoRepository(application);
        recoleccionRepository = new RecoleccionRepository(application);
        loadCatalogos();
    }

    public LiveData<List<String>> getClientes() {
        return clientesLiveData;
    }

    public LiveData<List<String>> getResiduos() {
        return residuosLiveData;
    }

    public LiveData<Boolean> getIsSaving() {
        return isSavingLiveData;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccessLiveData;
    }

    public void loadCatalogos() {
        catalogoRepository.obtenerClientes(clientes -> clientesLiveData.postValue(clientes));
        catalogoRepository.obtenerResiduos(residuos -> residuosLiveData.postValue(residuos));
    }

    public void guardarRecoleccion(String cliente, String residuo, double peso, double volumen, String obs) {
        isSavingLiveData.setValue(true);
        recoleccionRepository.guardarRecoleccion(cliente, residuo, peso, volumen, obs, success -> {
            isSavingLiveData.postValue(false);
            saveSuccessLiveData.postValue(success);
        });
    }
}
