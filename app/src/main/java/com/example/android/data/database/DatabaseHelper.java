package com.example.android.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.android.utils.Constants;
import com.example.android.utils.DateUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper.java — Administrador de la base de datos SQLite local.
 * Implementa el patrón Offline-First: todos los datos se guardan localmente
 * primero y se sincronizan con la API REST cuando hay conectividad.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "ECOLIM_DB";

    public DatabaseHelper(Context context) {
        super(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
    }

    // ============================================================
    // CREACIÓN DE TABLAS (onCreate)
    // ============================================================

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabla 1: Usuario — Credenciales para login offline
        db.execSQL("CREATE TABLE Usuario ("
                + "idUsuario INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "Correo TEXT, "
                + "Password TEXT);");

        // Tabla 2: Cliente — Catálogo de sedes/plantas
        db.execSQL("CREATE TABLE Cliente ("
                + "idCliente INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "RazonSocial TEXT);");

        // Tabla 3: Residuo — Catálogo de tipos de residuo
        db.execSQL("CREATE TABLE Residuo ("
                + "idResiduo INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "Nombre TEXT);");

        // Tabla 4: Recoleccion — Registro de cada operación
        db.execSQL("CREATE TABLE Recoleccion ("
                + "idRecoleccion INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "idCliente INTEGER, "
                + "idResiduo INTEGER, "
                + "Peso REAL, "
                + "Volumen REAL, "
                + "Observacion TEXT, "
                + "Fecha TEXT, "
                + "isSynced INTEGER DEFAULT 0, "
                + "FOREIGN KEY(idCliente) REFERENCES Cliente(idCliente), "
                + "FOREIGN KEY(idResiduo) REFERENCES Residuo(idResiduo));");

        // Tabla 5: Sincronizacion — Log de intentos de sincronización
        db.execSQL("CREATE TABLE Sincronizacion ("
                + "idSync INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "idRecoleccion INTEGER, "
                + "fechaIntento TEXT, "
                + "estadoSync TEXT, "
                + "respuestaApi TEXT, "
                + "FOREIGN KEY(idRecoleccion) REFERENCES Recoleccion(idRecoleccion));");

        // Insertar datos de prueba
        insertarDatosPrueba(db);
    }

    /**
     * Inserta datos iniciales para pruebas y demo.
     */
    private void insertarDatosPrueba(SQLiteDatabase db) {
        // Usuario administrador por defecto
        db.execSQL("INSERT INTO Usuario (Correo, Password) "
                + "VALUES ('admin@ecolim.com', '123456');");

        // Clientes de ejemplo
        db.execSQL("INSERT INTO Cliente (RazonSocial) VALUES "
                + "('Planta Industrial A'), "
                + "('Sede Logística B'), "
                + "('Fábrica Textil C');");

        // Residuos según clasificación NTP 900.058-2024
        db.execSQL("INSERT INTO Residuo (Nombre) VALUES "
                + "('Plásticos Peligrosos'), "
                + "('Metales Pesados'), "
                + "('Cartón Industrial'), "
                + "('Químicos');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Sincronizacion");
        db.execSQL("DROP TABLE IF EXISTS Recoleccion");
        db.execSQL("DROP TABLE IF EXISTS Residuo");
        db.execSQL("DROP TABLE IF EXISTS Cliente");
        db.execSQL("DROP TABLE IF EXISTS Usuario");
        onCreate(db);
    }

    // ============================================================
    // 1. AUTENTICACIÓN (Login)
    // ============================================================

    /**
     * Valida credenciales contra la tabla Usuario.
     * @return true si las credenciales son válidas
     */
    public boolean login(String correo, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM Usuario WHERE Correo=? AND Password=?",
                new String[]{correo, password});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    /**
     * Obtiene el nombre del usuario por correo (para SessionManager).
     * @return nombre del usuario o null si no existe
     */
    public String getNombreUsuario(String correo) {
        // En esta versión simplificada, retornamos el correo como nombre
        // En producción: SELECT nombre_Completo FROM Usuario WHERE Correo=?
        return correo.split("@")[0]; // "admin" de "admin@ecolim.com"
    }

    /**
     * Obtiene el ID del usuario por correo.
     * @return ID del usuario o -1 si no existe
     */
    public int getUsuarioId(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT idUsuario FROM Usuario WHERE Correo=?",
                new String[]{correo});
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    // ============================================================
    // 2. CATÁLOGOS (Clientes y Residuos)
    // ============================================================

    /**
     * Obtiene la lista de razones sociales de clientes.
     * @return Lista de strings para popular el AutoCompleteTextView
     */
    public List<String> obtenerClientes() {
        List<String> lista = new ArrayList<>();
        Cursor cursor = this.getReadableDatabase()
                .rawQuery("SELECT RazonSocial FROM Cliente", null);
        if (cursor.moveToFirst()) {
            do {
                lista.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

    /**
     * Obtiene la lista de nombres de residuos.
     * @return Lista de strings para popular el AutoCompleteTextView
     */
    public List<String> obtenerResiduos() {
        List<String> lista = new ArrayList<>();
        Cursor cursor = this.getReadableDatabase()
                .rawQuery("SELECT Nombre FROM Residuo", null);
        if (cursor.moveToFirst()) {
            do {
                lista.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

    /**
     * Busca el ID de un registro por nombre en una tabla de catálogo.
     * @return ID numérico o -1 si no se encuentra
     */
    private int getIdPorNombre(String tabla, String columnaId,
                                String columnaNombre, String valorNombre) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + columnaId + " FROM " + tabla
                        + " WHERE " + columnaNombre + "=?",
                new String[]{valorNombre});
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    // ============================================================
    // 3. REGISTRO DE RECOLECCIÓN (CRUD)
    // ============================================================

    /**
     * Guarda un nuevo registro de recolección en SQLite.
     * Siempre se guarda localmente primero (offline-first).
     * El campo isSynced=0 indica que está pendiente de sincronización.
     *
     * @param clienteNombre  Nombre del cliente (se resuelve a ID)
     * @param residuoNombre  Nombre del residuo (se resuelve a ID)
     * @param peso           Peso en kilogramos
     * @param volumen        Volumen en metros cúbicos
     * @param obs            Observaciones del operario
     * @return true si la inserción fue exitosa
     */
    public boolean guardarRecoleccion(String clienteNombre, String residuoNombre,
                                       double peso, double volumen, String obs) {
        // Resolver nombres a IDs numéricos
        int idCliente = getIdPorNombre("Cliente", "idCliente",
                "RazonSocial", clienteNombre);
        int idResiduo = getIdPorNombre("Residuo", "idResiduo",
                "Nombre", residuoNombre);

        // Validar que ambos catálogos existan
        if (idCliente == -1 || idResiduo == -1) {
            Log.e(TAG, "Cliente o Residuo no encontrado en catálogo");
            return false;
        }

        // Preparar valores para INSERT
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("idCliente", idCliente);
        values.put("idResiduo", idResiduo);
        values.put("Peso", peso);
        values.put("Volumen", volumen);
        values.put("Observacion", obs);
        values.put("Fecha", DateUtils.fechaActualDB());
        values.put("isSynced", 0); // Pendiente de sincronización

        long result = db.insert("Recoleccion", null, values);
        Log.i(TAG, "Recolección guardada con ID: " + result);
        return result != -1;
    }

    // ============================================================
    // 4. ESTADÍSTICAS (Dashboard)
    // ============================================================

    /**
     * Calcula el peso total acumulado de todas las recolecciones.
     * @return Peso total en kg
     */
    public double getTotalPeso() {
        Cursor cursor = this.getReadableDatabase()
                .rawQuery("SELECT COALESCE(SUM(Peso), 0) FROM Recoleccion", null);
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    /**
     * Calcula el volumen total acumulado de todas las recolecciones.
     * @return Volumen total en m³
     */
    public double getTotalVolumen() {
        Cursor cursor = this.getReadableDatabase()
                .rawQuery("SELECT COALESCE(SUM(Volumen), 0) FROM Recoleccion", null);
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    /**
     * Cuenta los registros pendientes de sincronización.
     * @return Cantidad de registros con isSynced = 0
     */
    public int getPendingCount() {
        Cursor cursor = this.getReadableDatabase()
                .rawQuery("SELECT COUNT(*) FROM Recoleccion WHERE isSynced = 0", null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /**
     * Cuenta el total de recolecciones registradas.
     * @return Cantidad total de registros
     */
    public int getTotalRegistros() {
        Cursor cursor = this.getReadableDatabase()
                .rawQuery("SELECT COUNT(*) FROM Recoleccion", null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // ============================================================
    // 5. REPORTES (Consultas con filtros dinámicos)
    // ============================================================

    /**
     * Obtiene reportes detallados con INNER JOIN y filtros dinámicos.
     * La query usa prepared statements para prevenir SQL injection.
     *
     * @param fechaFiltro    Fecha en formato YYYY-MM-DD (o vacío para todas)
     * @param residuoFiltro  Nombre del residuo (o "Todos" para todos)
     * @return Cursor con columnas: id, cliente, residuo, peso, volumen, fecha, isSynced
     */
    public Cursor obtenerReportes(String fechaFiltro, String residuoFiltro) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query base con INNER JOINs
        StringBuilder query = new StringBuilder(
                "SELECT r.idRecoleccion, c.RazonSocial, res.Nombre, "
                        + "r.Peso, r.Volumen, r.Fecha, r.isSynced "
                        + "FROM Recoleccion r "
                        + "INNER JOIN Cliente c ON r.idCliente = c.idCliente "
                        + "INNER JOIN Residuo res ON r.idResiduo = res.idResiduo "
                        + "WHERE 1=1 ");

        // Lista dinámica de argumentos para prepared statement
        List<String> args = new ArrayList<>();

        // Filtro por fecha (si se seleccionó)
        if (fechaFiltro != null && !fechaFiltro.isEmpty()) {
            query.append("AND r.Fecha = ? ");
            args.add(fechaFiltro);
        }

        // Filtro por tipo de residuo (si no es "Todos")
        if (residuoFiltro != null && !residuoFiltro.equals("Todos")) {
            query.append("AND res.Nombre = ? ");
            args.add(residuoFiltro);
        }

        // Ordenar por más recientes primero
        query.append("ORDER BY r.idRecoleccion DESC");

        return db.rawQuery(query.toString(), args.toArray(new String[0]));
    }

    // ============================================================
    // 6. SINCRONIZACIÓN CON API REST
    // ============================================================

    /**
     * Obtiene todos los registros pendientes de sincronización como JSONArray.
     * Este payload se envía al endpoint POST /api/v1/sync.
     *
     * @return JSONArray con los registros donde isSynced = 0
     */
    public JSONArray obtenerRegistrosPendientesJSON() {
        JSONArray jsonArray = new JSONArray();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT r.idRecoleccion, r.idCliente, r.idResiduo, "
                        + "r.Peso, r.Volumen, r.Observacion, r.Fecha "
                        + "FROM Recoleccion r WHERE r.isSynced = 0", null);

        if (cursor.moveToFirst()) {
            do {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("id_registro", cursor.getInt(0));
                    obj.put("id_usuario", 1); // TODO: usar SessionManager.getUserId()
                    obj.put("id_cliente", cursor.getInt(1));
                    obj.put("id_residuo", cursor.getInt(2));
                    obj.put("peso_kg", cursor.getDouble(3));
                    obj.put("volumen_m3", cursor.getDouble(4));
                    obj.put("observacion", cursor.getString(5));
                    obj.put("fecha_hora", cursor.getString(6));
                    jsonArray.put(obj);
                } catch (Exception e) {
                    Log.e(TAG, "Error al construir JSON: " + e.getMessage());
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return jsonArray;
    }

    /**
     * Marca una lista de registros como sincronizados (isSynced = 1).
     * Se invoca después de recibir confirmación de la API.
     *
     * @param ids Lista de IDs de recolección sincronizados exitosamente
     */
    public void marcarComoSincronizados(List<Integer> ids) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("isSynced", 1);

        for (int id : ids) {
            db.update("Recoleccion", values,
                    "idRecoleccion = ?", new String[]{String.valueOf(id)});

            // Registrar en tabla de log de sincronización
            ContentValues syncLog = new ContentValues();
            syncLog.put("idRecoleccion", id);
            syncLog.put("fechaIntento", DateUtils.fechaISO());
            syncLog.put("estadoSync", "EXITOSO");
            syncLog.put("respuestaApi", "HTTP 200 OK");
            db.insert("Sincronizacion", null, syncLog);
        }
        Log.i(TAG, ids.size() + " registros marcados como sincronizados");
    }

    /**
     * Interface de callback para operaciones asíncronas de sincronización.
     */
    public interface SyncCallback {
        void onResult(boolean success, int recordsSynced);
    }

    /**
     * Sincronización simulada (fallback cuando no hay API disponible).
     * Simula latencia de red de 2 segundos y marca todos los pendientes.
     */
    public void sincronizarConAPI(SyncCallback callback) {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simular latencia de red

                SQLiteDatabase db = this.getWritableDatabase();
                Cursor c = db.rawQuery(
                        "SELECT COUNT(*) FROM Recoleccion WHERE isSynced = 0", null);
                int count = 0;
                if (c.moveToFirst()) count = c.getInt(0);
                c.close();

                if (count > 0) {
                    Log.i(TAG, "Simulando POST /api/v1/sync con "
                            + count + " registros...");

                    // Marcar todos como sincronizados
                    ContentValues values = new ContentValues();
                    values.put("isSynced", 1);
                    db.update("Recoleccion", values, "isSynced = 0", null);
                }

                // Retornar al UI thread
                final int finalCount = count;
                new Handler(Looper.getMainLooper())
                        .post(() -> callback.onResult(true, finalCount));

            } catch (InterruptedException e) {
                new Handler(Looper.getMainLooper())
                        .post(() -> callback.onResult(false, 0));
            }
        }).start();
    }
}
