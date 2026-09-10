package com.example.android;

import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.example.android.ui.components.StatusCardView;
import com.example.android.ui.dashboard.DashboardViewModel;
import com.example.android.utils.SessionManager;

public class DashboardFragment extends Fragment {

    private TextView tvWelcome, tvPendingCount;
    private StatusCardView cardTotalPeso, cardTotalVolumen;
    private MaterialButton btnSync;
    private SessionManager sessionManager;
    private DashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        cardTotalPeso = view.findViewById(R.id.cardTotalPeso);
        cardTotalVolumen = view.findViewById(R.id.cardTotalVolumen);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        btnSync = view.findViewById(R.id.btnSyncNow);

        String nombre = sessionManager.getUserName();
        tvWelcome.setText("Hola, " + nombre);

        setupObservers();

        btnSync.setOnClickListener(v -> {
            String deviceId = "android-" + Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            viewModel.syncData(deviceId);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadStats();
    }

    private void setupObservers() {
        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                cardTotalPeso.setValue(String.format("%.1f", stats.totalPeso));
                cardTotalVolumen.setValue(String.format("%.1f", stats.totalVolumen));
                tvPendingCount.setText(stats.pendingCount + " registros locales");
            }
        });

        viewModel.getIsSyncing().observe(getViewLifecycleOwner(), isSyncing -> {
            if (isSyncing != null) {
                btnSync.setEnabled(!isSyncing);
                btnSync.setText(isSyncing ? "Sincronizando..." : "Sincronizar con API");
            }
        });

        viewModel.getSyncMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
