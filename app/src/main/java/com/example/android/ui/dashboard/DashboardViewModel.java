package com.example.android.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.android.data.repository.RecoleccionRepository;

public class DashboardViewModel extends AndroidViewModel {

    private final RecoleccionRepository repository;

    private final MutableLiveData<RecoleccionRepository.DashboardStats> statsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSyncingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> syncMessageLiveData = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = new RecoleccionRepository(application);
        loadStats();
    }

    public LiveData<RecoleccionRepository.DashboardStats> getStats() {
        return statsLiveData;
    }

    public LiveData<Boolean> getIsSyncing() {
        return isSyncingLiveData;
    }

    public LiveData<String> getSyncMessage() {
        return syncMessageLiveData;
    }

    public void loadStats() {
        repository.getDashboardStats(stats -> statsLiveData.postValue(stats));
    }

    public void syncData(String deviceId) {
        if (Boolean.TRUE.equals(isSyncingLiveData.getValue())) return;
        
        isSyncingLiveData.setValue(true);
        
        repository.sincronizarConAPI(deviceId, result -> {
            isSyncingLiveData.postValue(false);
            syncMessageLiveData.postValue(result.message);
            
            if (result.success) {
                loadStats();
            }
        });
    }
}
