package com.example.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.example.android.utils.Constants;

/**
 * ScannerFragment — Pantalla de validación y clasificación de residuos.
 * Ofrece dos métodos de identificación:
 * 1. Escaneo de código QR/barras con ZXing (para contenedores etiquetados)
 * 2. Clasificación manual por botones de peligrosidad (fallback visual)
 *
 * Al identificar un residuo, navega al RecoleccionFragment con el dato pre-llenado.
 */
public class ScannerFragment extends Fragment {

    private MaterialCardView cardCamera;
    private MaterialButton btnPeligroso, btnBiomedico, btnIndustrial;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Registrar launcher para resultado del escaneo QR/barras
        barcodeLauncher = registerForActivityResult(
                new ScanContract(),
                result -> {
                    if (result.getContents() != null) {
                        String codigoEscaneado = result.getContents();
                        procesarCodigoQR(codigoEscaneado);
                    } else {
                        Toast.makeText(getContext(), "Escaneo cancelado",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scanner, container, false);

        // Vincular vistas
        cardCamera = view.findViewById(R.id.cardCamera);
        btnPeligroso = view.findViewById(R.id.btnPeligroso);
        btnBiomedico = view.findViewById(R.id.btnBiomedico);
        btnIndustrial = view.findViewById(R.id.btnIndustrial);

        // Click en área de cámara → abrir escáner QR
        cardCamera.setOnClickListener(v -> iniciarEscaneo());

        // Botones de clasificación manual de peligrosidad
        btnPeligroso.setOnClickListener(v ->
                navegarConResiduo("Químicos"));
        btnBiomedico.setOnClickListener(v ->
                navegarConResiduo("Plásticos Peligrosos"));
        btnIndustrial.setOnClickListener(v ->
                navegarConResiduo("Cartón Industrial"));

        return view;
    }

    /**
     * Inicia la actividad de escaneo QR/barras usando ZXing.
     */
    private void iniciarEscaneo() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(
                ScanOptions.QR_CODE,    // Código QR
                ScanOptions.CODE_128,   // Código de barras 128
                ScanOptions.EAN_13      // Código de barras EAN-13
        );
        options.setPrompt("Escanea el código del contenedor de residuos");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCameraId(0); // Cámara trasera
        barcodeLauncher.launch(options);
    }

    /**
     * Procesa el código QR escaneado.
     * Formato esperado: "ECOLIM|{idResiduo}|{nombreResiduo}"
     * Si el formato no coincide, muestra el contenido raw.
     */
    private void procesarCodigoQR(String codigo) {
        // RegEx estricto: Debe comenzar por ECOLIM|, seguido de ID numérico (1-4 dígitos), 
        // seguido de | y un nombre alfanumérico. 
        // Esto evita inyecciones o lecturas basura.
        String patron = "^ECOLIM\\|\\d{1,4}\\|[\\w\\sáéíóúÁÉÍÓÚ]+$";
        
        if (codigo != null && codigo.matches(patron)) {
            String[] partes = codigo.split(Constants.QR_SEPARATOR);
            String nombreResiduo = partes[2];
            Toast.makeText(getContext(), "Residuo identificado: " + nombreResiduo,
                    Toast.LENGTH_SHORT).show();
            navegarConResiduo(nombreResiduo);
        } else {
            // Código no reconocido
            Toast.makeText(getContext(),
                    "Código inválido o malformado.\n(Formato no seguro)",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Navega al RecoleccionFragment pasando el nombre del residuo
     * identificado como argumento via Bundle.
     */
    private void navegarConResiduo(String nombreResiduo) {
        Bundle args = new Bundle();
        args.putString("nombre_residuo", nombreResiduo);

        NavHostFragment.findNavController(this)
                .navigate(R.id.nav_recoleccion, args);
    }
}
