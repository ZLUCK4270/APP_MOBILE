package com.example.android.ui.reportes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.android.data.repository.CatalogoRepository;
import com.example.android.data.repository.RecoleccionRepository;
import com.example.android.models.Reporte;

import java.util.List;

public class ReportesViewModel extends AndroidViewModel {

    private final CatalogoRepository catalogoRepository;
    private final RecoleccionRepository recoleccionRepository;

    private final MutableLiveData<List<String>> residuosLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Reporte>> reportesLiveData = new MutableLiveData<>();

    public ReportesViewModel(@NonNull Application application) {
        super(application);
        catalogoRepository = new CatalogoRepository(application);
        recoleccionRepository = new RecoleccionRepository(application);
        loadResiduos();
    }

    public LiveData<List<String>> getResiduos() {
        return residuosLiveData;
    }

    public LiveData<List<Reporte>> getReportes() {
        return reportesLiveData;
    }

    public void loadResiduos() {
        catalogoRepository.obtenerResiduos(residuos -> residuosLiveData.postValue(residuos));
    }

    public void loadReportes(String fecha, String residuo) {
        recoleccionRepository.obtenerReportes(fecha, residuo, reportes -> reportesLiveData.postValue(reportes));
    }
}
