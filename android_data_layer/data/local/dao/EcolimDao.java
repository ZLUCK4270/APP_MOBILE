package com.example.android.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.android.data.local.entity.ClienteEntity;
import com.example.android.data.local.entity.RecoleccionEntity;
import com.example.android.data.local.entity.ResiduoEntity;
import com.example.android.data.local.entity.SincronizacionEntity;
import com.example.android.data.local.entity.UsuarioEntity;
import com.example.android.models.Reporte;

import java.util.List;

@Dao
public interface EcolimDao {

    // --- Usuario ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUsuario(UsuarioEntity usuario);

    @Query("SELECT * FROM Usuario WHERE correo = :correo AND password = :password LIMIT 1")
    UsuarioEntity login(String correo, String password);

    @Query("SELECT * FROM Usuario WHERE correo = :correo LIMIT 1")
    UsuarioEntity getUsuarioByCorreo(String correo);

    // --- Cliente ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertClientes(List<ClienteEntity> clientes);

    @Query("SELECT razonSocial FROM Cliente")
    List<String> getNombresClientes();

    @Query("SELECT idCliente FROM Cliente WHERE razonSocial = :nombre LIMIT 1")
    int getClienteIdPorNombre(String nombre);

    // --- Residuo ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertResiduos(List<ResiduoEntity> residuos);

    @Query("SELECT nombre FROM Residuo")
    List<String> getNombresResiduos();

    @Query("SELECT idResiduo FROM Residuo WHERE nombre = :nombre LIMIT 1")
    int getResiduoIdPorNombre(String nombre);

    // --- Recoleccion ---
    @Insert
    long insertRecoleccion(RecoleccionEntity recoleccion);

    @Query("SELECT COALESCE(SUM(peso), 0) FROM Recoleccion")
    double getTotalPeso();

    @Query("SELECT COALESCE(SUM(volumen), 0) FROM Recoleccion")
    double getTotalVolumen();

    @Query("SELECT COUNT(*) FROM Recoleccion WHERE isSynced = 0")
    int getPendingCount();

    @Query("SELECT COUNT(*) FROM Recoleccion")
    int getTotalRegistros();

    @Query("SELECT * FROM Recoleccion WHERE isSynced = 0")
    List<RecoleccionEntity> getRegistrosPendientes();

    @Query("UPDATE Recoleccion SET isSynced = 1 WHERE idRecoleccion IN (:ids)")
    void marcarComoSincronizados(List<Integer> ids);

    @Query("UPDATE Recoleccion SET isSynced = 1 WHERE isSynced = 0")
    void marcarTodosComoSincronizados();

    // --- Reportes (Join) ---
    @Query("SELECT r.idRecoleccion, c.razonSocial AS cliente, res.nombre AS residuo, " +
           "r.peso, r.volumen, r.fecha, (r.isSynced = 1) AS isSynced " +
           "FROM Recoleccion r " +
           "INNER JOIN Cliente c ON r.idCliente = c.idCliente " +
           "INNER JOIN Residuo res ON r.idResiduo = res.idResiduo " +
           "WHERE (:fechaFiltro IS NULL OR :fechaFiltro = '' OR r.fecha = :fechaFiltro) " +
           "AND (:residuoFiltro IS NULL OR :residuoFiltro = 'Todos' OR res.nombre = :residuoFiltro) " +
           "ORDER BY r.idRecoleccion DESC")
    List<Reporte> obtenerReportes(String fechaFiltro, String residuoFiltro);

    // --- Sincronizacion ---
    @Insert
    void insertSyncLog(SincronizacionEntity log);
}
