package com.example.android;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.example.android.models.Reporte;
import com.example.android.ui.reportes.ReportesViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReportesFragment extends Fragment {

    private MaterialButton btnFechaFiltro, btnLimpiarFiltros, btnExportar;
    private AutoCompleteTextView actvResiduoFiltro;
    private RecyclerView rvReportes;
    private ReporteAdapter adapter;
    private String fechaSeleccionada = "";
    private ReportesViewModel viewModel;
    
    private List<Reporte> reportesActuales = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        viewModel = new ViewModelProvider(this).get(ReportesViewModel.class);

        btnFechaFiltro = view.findViewById(R.id.btnFechaFiltro);
        btnLimpiarFiltros = view.findViewById(R.id.btnLimpiarFiltros);
        actvResiduoFiltro = view.findViewById(R.id.spinnerResiduo);
        btnExportar = view.findViewById(R.id.btnExportar);
        rvReportes = view.findViewById(R.id.rvReportes);

        rvReportes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReporteAdapter(new ArrayList<>());
        rvReportes.setAdapter(adapter);

        setupObservers();
        configurarListeners();

        return view;
    }

    private void setupObservers() {
        viewModel.getResiduos().observe(getViewLifecycleOwner(), residuos -> {
            if (residuos != null) {
                residuos.add(0, "Todos");
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        residuos);
                actvResiduoFiltro.setAdapter(spinnerAdapter);
                actvResiduoFiltro.setText("Todos", false);
                cargarReportes();
            }
        });

        viewModel.getReportes().observe(getViewLifecycleOwner(), reportes -> {
            if (reportes != null) {
                reportesActuales = reportes;
                adapter.actualizarDatos(reportes);
            }
        });
    }

    private void configurarListeners() {
        btnFechaFiltro.setOnClickListener(v -> mostrarDatePicker());

        btnLimpiarFiltros.setOnClickListener(v -> {
            fechaSeleccionada = "";
            btnFechaFiltro.setText("Fecha");
            actvResiduoFiltro.setText("Todos", false);
            cargarReportes();
        });

        actvResiduoFiltro.setOnItemClickListener((parent, view, position, id) ->
                cargarReportes());

        btnExportar.setOnClickListener(v -> generarYCompartirPDF());
    }

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

    private void cargarReportes() {
        String residuoFiltro = actvResiduoFiltro.getText().toString();
        viewModel.loadReportes(fechaSeleccionada, residuoFiltro);
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarReportes();
    }

    private void generarYCompartirPDF() {
        if (adapter.getItemCount() == 0 || reportesActuales.isEmpty()) {
            Toast.makeText(getContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        paint.setTextSize(24);
        paint.setColor(Color.BLACK);
        canvas.drawText("Reporte de Recolección de Residuos", 40, 50, paint);
        
        paint.setTextSize(14);
        canvas.drawText("Filtros: Fecha=" + (fechaSeleccionada.isEmpty() ? "Todas" : fechaSeleccionada) + 
                        ", Tipo=" + actvResiduoFiltro.getText().toString(), 40, 80, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        canvas.drawText("ID", 40, 120, paint);
        canvas.drawText("Fecha", 80, 120, paint);
        canvas.drawText("Cliente", 180, 120, paint);
        canvas.drawText("Residuo", 320, 120, paint);
        canvas.drawText("Peso/Vol", 460, 120, paint);
        paint.setFakeBoldText(false);

        int y = 140;
        for (Reporte cursor : reportesActuales) {
            if (y > 800) {
                canvas.drawText("... más registros (PDF truncado)", 40, y, paint);
                break;
            }
            canvas.drawText(String.valueOf(cursor.getIdRecoleccion()), 40, y, paint);
            canvas.drawText(cursor.getFecha(), 80, y, paint);
            
            String cliente = cursor.getCliente();
            if(cliente.length() > 15) cliente = cliente.substring(0, 15) + "...";
            canvas.drawText(cliente, 180, y, paint);
            
            String residuo = cursor.getResiduo();
            if(residuo.length() > 15) residuo = residuo.substring(0, 15) + "...";
            canvas.drawText(residuo, 320, y, paint);
            
            canvas.drawText(cursor.getPeso() + "kg / " + cursor.getVolumen() + "m3", 460, y, paint);
            y += 20;
        }

        document.finishPage(page);

        try {
            File pdfDir = new File(requireContext().getCacheDir(), "reportes");
            if (!pdfDir.exists()) pdfDir.mkdirs();
            File file = new File(pdfDir, "Ecolim_Reporte_" + System.currentTimeMillis() + ".pdf");
            
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

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
