package com.example.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.android.models.Reporte;
import java.util.List;

/**
 * ReporteAdapter — Adapter para el RecyclerView de reportes.
 * Muestra cada registro de recolección en un MaterialCardView
 * con indicador visual de estado de sincronización.
 */
public class ReporteAdapter extends RecyclerView.Adapter<ReporteAdapter.ReporteViewHolder> {

    private List<Reporte> listaReportes;

    public ReporteAdapter(List<Reporte> listaReportes) {
        this.listaReportes = listaReportes;
    }

    /**
     * Actualiza la lista de datos y refresca el RecyclerView.
     */
    public void actualizarDatos(List<Reporte> nuevaLista) {
        this.listaReportes = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReporteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte, parent, false);
        return new ReporteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReporteViewHolder holder, int position) {
        Reporte reporte = listaReportes.get(position);

        // Asignar datos a las vistas
        holder.tvFecha.setText(reporte.getFecha());
        holder.tvCliente.setText(reporte.getCliente());
        holder.tvResiduo.setText(reporte.getResiduo());
        holder.tvPeso.setText("⚖️ " + reporte.getPeso() + " kg");
        holder.tvVolumen.setText("📦 " + reporte.getVolumen() + " m³");

        // Indicador visual de estado de sincronización
        if (reporte.isSynced()) {
            holder.tvSyncStatus.setText("Sincronizado");
            holder.tvSyncStatus.setTextColor(0xFF2E7D32); // Verde oscuro
            holder.tvSyncStatus.setBackgroundResource(R.drawable.bg_status_synced);
        } else {
            holder.tvSyncStatus.setText("Pendiente");
            holder.tvSyncStatus.setTextColor(0xFFD32F2F); // Rojo
            holder.tvSyncStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }
    }

    @Override
    public int getItemCount() {
        return listaReportes.size();
    }

    /**
     * ViewHolder para cada item del RecyclerView.
     */
    static class ReporteViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvCliente, tvResiduo, tvPeso, tvVolumen, tvSyncStatus;

        public ReporteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvResiduo = itemView.findViewById(R.id.tvResiduo);
            tvPeso = itemView.findViewById(R.id.tvPeso);
            tvVolumen = itemView.findViewById(R.id.tvVolumen);
            tvSyncStatus = itemView.findViewById(R.id.tvSyncStatus);
        }
    }
}
