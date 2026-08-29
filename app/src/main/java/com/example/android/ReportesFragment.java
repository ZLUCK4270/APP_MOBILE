package com.example.android;

import android.app.DatePickerDialog;
import android.database.Cursor;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.example.android.data.database.DatabaseHelper;
import com.example.android.models.Reporte;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * ReportesFragment — Historial de recolecciones con filtros dinámicos.
 * Muestra los registros en un RecyclerView con filtros por fecha y tipo de residuo.
 * Incluye botón para exportar/compartir reportes.
 */
public class ReportesFragment extends Fragment {

    private MaterialButton btnFechaFiltro, btnLimpiarFiltros, btnExportar;
    private AutoCompleteTextView actvResiduoFiltro;
    private RecyclerView rvReportes;
    private ReporteAdapter adapter;
    private DatabaseHelper dbHelper;
    private String fechaSeleccionada = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        // Inicializar componentes
        dbHelper = new DatabaseHelper(requireContext());
        btnFechaFiltro = view.findViewById(R.id.btnFechaFiltro);
        btnLimpiarFiltros = view.findViewById(R.id.btnLimpiarFiltros);
        actvResiduoFiltro = view.findViewById(R.id.spinnerResiduo);
        btnExportar = view.findViewById(R.id.btnExportar);
        rvReportes = view.findViewById(R.id.rvReportes);

        // Configurar RecyclerView
        rvReportes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReporteAdapter(new ArrayList<>());
        rvReportes.setAdapter(adapter);

        // Cargar filtros y datos
        cargarFiltroResiduos();
        configurarListeners();
        cargarReportes();

        return view;
    }

    /**
     * Carga el dropdown de tipos de residuo con opción "Todos" por defecto.
     */
    private void cargarFiltroResiduos() {
        List<String> residuos = dbHelper.obtenerResiduos();
        residuos.add(0, "Todos"); // Opción por defecto
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                residuos);
        actvResiduoFiltro.setAdapter(spinnerAdapter);
        actvResiduoFiltro.setText("Todos", false);
    }

    /**
     * Configura los listeners de los botones de filtro.
     */
    private void configurarListeners() {
        // Filtro de fecha: abre DatePickerDialog
        btnFechaFiltro.setOnClickListener(v -> mostrarDatePicker());

        // Limpiar todos los filtros
        btnLimpiarFiltros.setOnClickListener(v -> {
            fechaSeleccionada = "";
            btnFechaFiltro.setText("Fecha");
            actvResiduoFiltro.setText("Todos", false);
            cargarReportes();
        });

        // Filtro de residuo: recargar al seleccionar
        actvResiduoFiltro.setOnItemClickListener((parent, view, position, id) ->
                cargarReportes());

        // Botón exportar/compartir
        btnExportar.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    "Generando reporte PDF...\n(Requiere conexión con API)",
                    Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Muestra un DatePickerDialog para seleccionar fecha de filtro.
     */
    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year1, month1, dayOfMonth) -> {
                    fechaSeleccionada = String.format("%04d-%02d-%02d",
                            year1, month1 + 1, dayOfMonth);
                    btnFechaFiltro.setText(fechaSeleccionada);
                    cargarReportes();
                }, year, month, day);
        datePickerDialog.show();
    }

    /**
     * Ejecuta la query filtrada y actualiza el RecyclerView.
     */
    private void cargarReportes() {
        String residuoFiltro = actvResiduoFiltro.getText().toString();
        Cursor cursor = dbHelper.obtenerReportes(fechaSeleccionada, residuoFiltro);
        List<Reporte> lista = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                Reporte r = new Reporte(
                        cursor.getInt(0),       // idRecoleccion
                        cursor.getString(1),    // Cliente (RazonSocial)
                        cursor.getString(2),    // Residuo (Nombre)
                        cursor.getDouble(3),    // Peso
                        cursor.getDouble(4),    // Volumen
                        cursor.getString(5),    // Fecha
                        cursor.getInt(6) == 1   // isSynced
                );
                lista.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.actualizarDatos(lista);
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarReportes(); // Refrescar al volver a esta pestaña
    }
}
