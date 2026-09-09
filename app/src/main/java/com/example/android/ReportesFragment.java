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
import android.graphics.pdf.PdfDocument;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Color;
import java.io.File;
import java.io.FileOutputStream;
import android.net.Uri;
import androidx.core.content.FileProvider;
import android.content.Intent;


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
            generarYCompartirPDF();
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

    /**
     * Genera un reporte PDF con los datos actuales del RecyclerView y abre 
     * un Intent para compartirlo usando FileProvider.
     */
    private void generarYCompartirPDF() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(getContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        // Título
        paint.setTextSize(24);
        paint.setColor(Color.BLACK);
        canvas.drawText("Reporte de Recolección de Residuos", 40, 50, paint);
        
        // Filtros aplicados
        paint.setTextSize(14);
        canvas.drawText("Filtros: Fecha=" + (fechaSeleccionada.isEmpty() ? "Todas" : fechaSeleccionada) + 
                        ", Tipo=" + actvResiduoFiltro.getText().toString(), 40, 80, paint);

        // Cabecera tabla
        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        canvas.drawText("ID", 40, 120, paint);
        canvas.drawText("Fecha", 80, 120, paint);
        canvas.drawText("Cliente", 180, 120, paint);
        canvas.drawText("Residuo", 320, 120, paint);
        canvas.drawText("Peso/Vol", 460, 120, paint);
        paint.setFakeBoldText(false);

        // Datos
        int y = 140;
        Cursor cursor = dbHelper.obtenerReportes(fechaSeleccionada, actvResiduoFiltro.getText().toString());
        if (cursor.moveToFirst()) {
            do {
                if (y > 800) {
                    // Manejo simple de paginación o corte (simplificado para MVP)
                    canvas.drawText("... más registros (PDF truncado)", 40, y, paint);
                    break;
                }
                canvas.drawText(String.valueOf(cursor.getInt(0)), 40, y, paint);
                canvas.drawText(cursor.getString(5), 80, y, paint);
                String cliente = cursor.getString(1);
                if(cliente.length() > 15) cliente = cliente.substring(0, 15) + "...";
                canvas.drawText(cliente, 180, y, paint);
                String residuo = cursor.getString(2);
                if(residuo.length() > 15) residuo = residuo.substring(0, 15) + "...";
                canvas.drawText(residuo, 320, y, paint);
                canvas.drawText(cursor.getDouble(3) + "kg / " + cursor.getDouble(4) + "m3", 460, y, paint);
                y += 20;
            } while (cursor.moveToNext());
        }
        cursor.close();

        document.finishPage(page);

        // Guardar archivo en caché
        try {
            File pdfDir = new File(requireContext().getCacheDir(), "reportes");
            if (!pdfDir.exists()) pdfDir.mkdirs();
            File file = new File(pdfDir, "Ecolim_Reporte_" + System.currentTimeMillis() + ".pdf");
            
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            // Compartir con FileProvider
            Uri pdfUri = FileProvider.getUriForFile(requireContext(), 
                    requireContext().getApplicationContext().getPackageName() + ".fileprovider", 
                    file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Compartir Reporte PDF"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            document.close();
        }
    }
}
