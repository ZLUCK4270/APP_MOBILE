package com.example.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.android.data.database.DatabaseHelper;
import com.example.android.utils.Constants;

/**
 * RecoleccionFragment — Formulario de registro de recolección de residuos.
 * Permite al operario seleccionar cliente, tipo de residuo, ingresar peso/volumen
 * y observaciones. Incluye validación completa antes de guardar.
 */
public class RecoleccionFragment extends Fragment {

    AutoCompleteTextView actvCliente, actvResiduo;
    TextInputEditText etPeso, etVolumen, etObservacion;
    MaterialButton btnGuardar;
    DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recoleccion, container, false);

        // Inicializar helper de base de datos
        dbHelper = new DatabaseHelper(requireContext());

        // Vincular vistas del formulario
        actvCliente = view.findViewById(R.id.actvCliente);
        actvResiduo = view.findViewById(R.id.actvResiduo);
        etPeso = view.findViewById(R.id.etPeso);
        etVolumen = view.findViewById(R.id.etVolumen);
        etObservacion = view.findViewById(R.id.etObservacion);
        btnGuardar = view.findViewById(R.id.btnGuardarRecoleccion);

        // Popular dropdowns con datos de SQLite
        cargarCatalogos();

        // Verificar si se recibió un residuo desde el Scanner (QR)
        if (getArguments() != null) {
            String residuoEscaneado = getArguments().getString("nombre_residuo");
            if (residuoEscaneado != null && !residuoEscaneado.isEmpty()) {
                actvResiduo.setText(residuoEscaneado, false);
                Toast.makeText(getContext(), "Residuo detectado: " + residuoEscaneado,
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Listener del botón guardar
        btnGuardar.setOnClickListener(v -> guardarDatos());

        return view;
    }

    /**
     * Carga los catálogos de clientes y residuos en los dropdowns.
     */
    private void cargarCatalogos() {
        ArrayAdapter<String> adapterClientes = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                dbHelper.obtenerClientes());
        actvCliente.setAdapter(adapterClientes);

        ArrayAdapter<String> adapterResiduos = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                dbHelper.obtenerResiduos());
        actvResiduo.setAdapter(adapterResiduos);
    }

    /**
     * Valida todos los campos y guarda la recolección en SQLite.
     */
    private void guardarDatos() {
        // Ejecutar validación completa
        if (!validarFormulario()) {
            return; // Hay errores, no guardar
        }

        try {
            String cliente = actvCliente.getText().toString().trim();
            String residuo = actvResiduo.getText().toString().trim();
            double peso = Double.parseDouble(etPeso.getText().toString().trim());
            double volumen = Double.parseDouble(etVolumen.getText().toString().trim());
            String obs = etObservacion.getText().toString().trim();

            // Guardar en SQLite (offline-first, isSynced=0)
            boolean exito = dbHelper.guardarRecoleccion(cliente, residuo,
                    peso, volumen, obs);

            if (exito) {
                Toast.makeText(getContext(), "✓ Recolección guardada localmente",
                        Toast.LENGTH_SHORT).show();
                limpiarFormulario();
            } else {
                Toast.makeText(getContext(), "Error al guardar. Verifica los datos.",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Error en valores numéricos",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Validación completa del formulario con 5 reglas de negocio.
     * @return true si todos los campos son válidos
     */
    private boolean validarFormulario() {
        boolean esValido = true;

        // REGLA 1: Cliente obligatorio y debe existir en catálogo
        String cliente = actvCliente.getText().toString().trim();
        if (cliente.isEmpty()) {
            actvCliente.setError("Seleccione un cliente");
            esValido = false;
        } else if (!dbHelper.obtenerClientes().contains(cliente)) {
            actvCliente.setError("Cliente no válido. Seleccione de la lista");
            esValido = false;
        }

        // REGLA 2: Residuo obligatorio y debe existir en catálogo
        String residuo = actvResiduo.getText().toString().trim();
        if (residuo.isEmpty()) {
            actvResiduo.setError("Seleccione un tipo de residuo");
            esValido = false;
        } else if (!dbHelper.obtenerResiduos().contains(residuo)) {
            actvResiduo.setError("Residuo no válido. Seleccione de la lista");
            esValido = false;
        }

        // REGLA 3: Peso numérico positivo dentro de rango
        String pesoStr = etPeso.getText().toString().trim();
        if (pesoStr.isEmpty()) {
            etPeso.setError("Ingrese el peso");
            esValido = false;
        } else {
            try {
                double peso = Double.parseDouble(pesoStr);
                if (peso < Constants.PESO_MIN || peso > Constants.PESO_MAX) {
                    etPeso.setError("Peso debe estar entre "
                            + Constants.PESO_MIN + " y " + Constants.PESO_MAX + " kg");
                    esValido = false;
                }
            } catch (NumberFormatException e) {
                etPeso.setError("Ingrese un valor numérico válido");
                esValido = false;
            }
        }

        // REGLA 4: Volumen numérico positivo dentro de rango
        String volumenStr = etVolumen.getText().toString().trim();
        if (volumenStr.isEmpty()) {
            etVolumen.setError("Ingrese el volumen");
            esValido = false;
        } else {
            try {
                double volumen = Double.parseDouble(volumenStr);
                if (volumen < Constants.VOLUMEN_MIN || volumen > Constants.VOLUMEN_MAX) {
                    etVolumen.setError("Volumen debe estar entre "
                            + Constants.VOLUMEN_MIN + " y " + Constants.VOLUMEN_MAX + " m³");
                    esValido = false;
                }
            } catch (NumberFormatException e) {
                etVolumen.setError("Ingrese un valor numérico válido");
                esValido = false;
            }
        }

        // REGLA 5: Coherencia peso/volumen (densidad razonable)
        if (esValido && !pesoStr.isEmpty() && !volumenStr.isEmpty()) {
            try {
                double peso = Double.parseDouble(pesoStr);
                double volumen = Double.parseDouble(volumenStr);
                if (volumen > 0) {
                    double densidad = peso / volumen;
                    if (densidad < Constants.DENSIDAD_MIN
                            || densidad > Constants.DENSIDAD_MAX) {
                        etPeso.setError("La relación peso/volumen parece incorrecta");
                        esValido = false;
                    }
                }
            } catch (Exception ignored) { /* Ya validado arriba */ }
        }

        return esValido;
    }

    /**
     * Limpia todos los campos del formulario tras guardar exitosamente.
     */
    private void limpiarFormulario() {
        etPeso.setText("");
        etVolumen.setText("");
        etObservacion.setText("");
        actvCliente.setText("");
        actvResiduo.setText("");
        actvCliente.requestFocus();
    }
}
