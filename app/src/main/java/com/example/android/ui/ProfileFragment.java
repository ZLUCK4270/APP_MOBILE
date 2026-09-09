package com.example.android.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.android.R;
import com.example.android.api.ApiClient;
import com.example.android.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    private TextInputEditText etNombre, etEmail;
    private Button btnSave;
    private ProgressBar pbProfile;
    private SessionManager sessionManager;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        etNombre = view.findViewById(R.id.etProfileNombre);
        etEmail = view.findViewById(R.id.etProfileEmail);
        btnSave = view.findViewById(R.id.btnProfileSave);
        pbProfile = view.findViewById(R.id.pbProfile);

        // Cargar datos actuales
        String nombreCompleto = sessionManager.getUserName();
        etNombre.setText(nombreCompleto);
        etEmail.setText(sessionManager.getUserEmail());

        btnSave.setOnClickListener(v -> guardarCambios());
    }

    private void guardarCambios() {
        String nuevoNombre = etNombre.getText().toString().trim();
        if (nuevoNombre.isEmpty()) {
            etNombre.setError("El nombre no puede estar vacío");
            return;
        }

        // Separa el nombre y apellido asumiendo que el primer espacio divide
        String[] partes = nuevoNombre.split(" ", 2);
        String nombre = partes[0];
        String apellido = partes.length > 1 ? partes[1] : "";

        pbProfile.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        try {
            JSONObject payload = new JSONObject();
            payload.put("nombre", nombre);
            payload.put("apellido", apellido);

            ApiClient.actualizarPerfil(sessionManager.getUserId(), payload, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String responseBody) {
                    if (isAdded()) {
                        pbProfile.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        
                        // Actualizar SharedPreferences
                        sessionManager.crearSesion(
                                sessionManager.getUserId(),
                                nuevoNombre,
                                sessionManager.getUserEmail(),
                                sessionManager.getUserRol()
                        );
                        
                        Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (isAdded()) {
                        pbProfile.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Log.e("Profile", "Error: " + errorMessage);
                        Toast.makeText(requireContext(), "Error al actualizar (Intenta conectarte a internet)", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            pbProfile.setVisibility(View.GONE);
            btnSave.setEnabled(true);
        }
    }
}
