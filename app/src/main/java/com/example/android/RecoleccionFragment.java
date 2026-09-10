package com.example.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import com.example.android.ui.recoleccion.RecoleccionViewModel;
import com.example.android.utils.Constants;
import com.example.android.utils.ResiduoClassifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecoleccionFragment extends Fragment {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;

    private AutoCompleteTextView actvCliente, actvResiduo;
    private TextInputEditText etPeso, etVolumen, etObservacion;
    private MaterialButton btnGuardar;
    private RecoleccionViewModel viewModel;

    // ML Kit & CameraX
    private MaterialCardView cardScan;
    private LinearLayout llScanPlaceholder;
    private PreviewView viewFinder;
    private TextView tvOverlayResult;
    private ExecutorService cameraExecutor;
    private ImageLabeler labeler;
    private ProcessCameraProvider cameraProvider;

    private boolean isUpdatingMedidas = false;
    private boolean isCameraActive = false;

    private List<String> clientesLocales = new ArrayList<>();
    private List<String> residuosLocales = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recoleccion, container, false);

        viewModel = new ViewModelProvider(this).get(RecoleccionViewModel.class);

        actvCliente = view.findViewById(R.id.actvCliente);
        actvResiduo = view.findViewById(R.id.actvResiduo);
        etPeso = view.findViewById(R.id.etPeso);
        etVolumen = view.findViewById(R.id.etVolumen);
        etObservacion = view.findViewById(R.id.etObservacion);
        btnGuardar = view.findViewById(R.id.btnGuardarRecoleccion);

        cardScan = view.findViewById(R.id.cardScan);
        llScanPlaceholder = view.findViewById(R.id.llScanPlaceholder);
        viewFinder = view.findViewById(R.id.viewFinder);
        tvOverlayResult = view.findViewById(R.id.tvOverlayResult);

        cameraExecutor = Executors.newSingleThreadExecutor();
        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        setupObservers();

        btnGuardar.setOnClickListener(v -> guardarDatos());
        setupAutoCalculation();

        cardScan.setOnClickListener(v -> {
            if (!isCameraActive) {
                checkCameraPermissionAndStart();
            } else {
                stopCamera();
            }
        });

        return view;
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(getContext(), "Permiso de cámara requerido para escanear", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        isCameraActive = true;
        llScanPlaceholder.setVisibility(View.GONE);
        viewFinder.setVisibility(View.VISIBLE);
        tvOverlayResult.setVisibility(View.VISIBLE);
        tvOverlayResult.setText("Buscando objetos...");

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(getContext(), "Error al abrir la cámara", Toast.LENGTH_SHORT).show();
                stopCamera();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void analyzeImage(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        
        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    for (ImageLabel label : labels) {
                        float confidence = label.getConfidence();
                        if (confidence > 0.70f) { // 70% confidence minimum
                            ResiduoClassifier.ClassificationResult result = ResiduoClassifier.classify(label.getText());
                            if (result != null) {
                                requireActivity().runOnUiThread(() -> {
                                    tvOverlayResult.setText("¡" + result.categoria + " detectado! (" + result.propiedad + ")");
                                    
                                    // Make sure it matches our spinner list, or close enough
                                    if (residuosLocales.contains(result.categoria) || true) { // Optional strict check
                                        actvResiduo.setText(result.categoria, false);
                                        stopCamera();
                                        etPeso.requestFocus();
                                    }
                                });
                                break;
                            }
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        isCameraActive = false;
        requireActivity().runOnUiThread(() -> {
            viewFinder.setVisibility(View.GONE);
            tvOverlayResult.setVisibility(View.GONE);
            llScanPlaceholder.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        if (labeler != null) {
            labeler.close();
        }
    }

    // --- LOGICA DE NEGOCIO ---

    private void setupAutoCalculation() {
        etPeso.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingMedidas) return;
                try {
                    if (s.toString().trim().isEmpty()) {
                        isUpdatingMedidas = true;
                        etVolumen.setText("");
                        isUpdatingMedidas = false;
                        return;
                    }
                    double peso = Double.parseDouble(s.toString());
                    double volumen = peso / Constants.DENSIDAD_POR_DEFECTO;
                    isUpdatingMedidas = true;
                    etVolumen.setText(String.format(java.util.Locale.US, "%.2f", volumen));
                    isUpdatingMedidas = false;
                } catch (NumberFormatException ignored) {}
            }
        });

        etVolumen.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingMedidas) return;
                try {
                    if (s.toString().trim().isEmpty()) {
                        isUpdatingMedidas = true;
                        etPeso.setText("");
                        isUpdatingMedidas = false;
                        return;
                    }
                    double volumen = Double.parseDouble(s.toString());
                    double peso = volumen * Constants.DENSIDAD_POR_DEFECTO;
                    isUpdatingMedidas = true;
                    etPeso.setText(String.format(java.util.Locale.US, "%.2f", peso));
                    isUpdatingMedidas = false;
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private void setupObservers() {
        viewModel.getClientes().observe(getViewLifecycleOwner(), clientes -> {
            if (clientes != null) {
                clientesLocales = clientes;
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        clientesLocales);
                actvCliente.setAdapter(adapter);
            }
        });

        viewModel.getResiduos().observe(getViewLifecycleOwner(), residuos -> {
            if (residuos != null) {
                residuosLocales = residuos;
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        residuosLocales);
                actvResiduo.setAdapter(adapter);
            }
        });

        viewModel.getIsSaving().observe(getViewLifecycleOwner(), isSaving -> {
            if (isSaving != null) {
                btnGuardar.setEnabled(!isSaving);
            }
        });

        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (success) {
                    Toast.makeText(getContext(), "✓ Recolección guardada localmente", Toast.LENGTH_SHORT).show();
                    limpiarFormulario();
                } else {
                    Toast.makeText(getContext(), "Error al guardar. Verifica los datos.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void guardarDatos() {
        if (!validarFormulario()) {
            return;
        }

        try {
            String cliente = actvCliente.getText().toString().trim();
            String residuo = actvResiduo.getText().toString().trim();
            double peso = Double.parseDouble(etPeso.getText().toString().trim());
            double volumen = Double.parseDouble(etVolumen.getText().toString().trim());
            String obs = etObservacion.getText().toString().trim();

            viewModel.guardarRecoleccion(cliente, residuo, peso, volumen, obs);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Error en valores numéricos", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validarFormulario() {
        boolean esValido = true;

        String cliente = actvCliente.getText().toString().trim();
        if (cliente.isEmpty()) {
            actvCliente.setError("Seleccione un cliente");
            esValido = false;
        } else if (!clientesLocales.contains(cliente)) {
            actvCliente.setError("Cliente no válido. Seleccione de la lista");
            esValido = false;
        }

        String residuo = actvResiduo.getText().toString().trim();
        if (residuo.isEmpty()) {
            actvResiduo.setError("Seleccione un tipo de residuo");
            esValido = false;
        } // Quitamos la validación estricta contains(residuo) para permitir IA
        
        String pesoStr = etPeso.getText().toString().trim();
        if (pesoStr.isEmpty()) {
            etPeso.setError("Ingrese el peso");
            esValido = false;
        } else {
            try {
                double peso = Double.parseDouble(pesoStr);
                if (peso < Constants.PESO_MIN || peso > Constants.PESO_MAX) {
                    etPeso.setError("Peso debe estar entre " + Constants.PESO_MIN + " y " + Constants.PESO_MAX + " kg");
                    esValido = false;
                }
            } catch (NumberFormatException e) {
                etPeso.setError("Ingrese un valor numérico válido");
                esValido = false;
            }
        }

        String volumenStr = etVolumen.getText().toString().trim();
        if (volumenStr.isEmpty()) {
            etVolumen.setError("Ingrese el volumen");
            esValido = false;
        } else {
            try {
                double volumen = Double.parseDouble(volumenStr);
                if (volumen < Constants.VOLUMEN_MIN || volumen > Constants.VOLUMEN_MAX) {
                    etVolumen.setError("Volumen debe estar entre " + Constants.VOLUMEN_MIN + " y " + Constants.VOLUMEN_MAX + " m³");
                    esValido = false;
                }
            } catch (NumberFormatException e) {
                etVolumen.setError("Ingrese un valor numérico válido");
                esValido = false;
            }
        }

        if (esValido && !pesoStr.isEmpty() && !volumenStr.isEmpty()) {
            try {
                double peso = Double.parseDouble(pesoStr);
                double volumen = Double.parseDouble(volumenStr);
                if (volumen > 0) {
                    double densidad = peso / volumen;
                    if (densidad < Constants.DENSIDAD_MIN || densidad > Constants.DENSIDAD_MAX) {
                        etPeso.setError("La relación peso/volumen parece incorrecta");
                        esValido = false;
                    }
                }
            } catch (Exception ignored) { }
        }

        return esValido;
    }

    private void limpiarFormulario() {
        etPeso.setText("");
        etVolumen.setText("");
        etObservacion.setText("");
        actvCliente.setText("");
        actvResiduo.setText("");
        actvCliente.requestFocus();
    }
}
