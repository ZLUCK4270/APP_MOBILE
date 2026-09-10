package com.example.android.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.android.data.local.dao.EcolimDao;
import com.example.android.data.local.entity.ClienteEntity;
import com.example.android.data.local.entity.RecoleccionEntity;
import com.example.android.data.local.entity.ResiduoEntity;
import com.example.android.data.local.entity.SincronizacionEntity;
import com.example.android.data.local.entity.UsuarioEntity;
import com.example.android.utils.Constants;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        UsuarioEntity.class,
        ClienteEntity.class,
        ResiduoEntity.class,
        RecoleccionEntity.class,
        SincronizacionEntity.class
}, version = 2, exportSchema = false)
public abstract class EcolimDatabase extends RoomDatabase {

    public abstract EcolimDao ecolimDao();

    private static volatile EcolimDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static EcolimDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (EcolimDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            EcolimDatabase.class, Constants.DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                EcolimDao dao = INSTANCE.ecolimDao();
                
                // Insert default admin
                UsuarioEntity admin = new UsuarioEntity();
                admin.nombre = "Administrador";
                admin.correo = "admin@ecolim.com";
                admin.password = "123456";
                admin.rol = "ADMIN";
                dao.insertUsuario(admin);

                // Insert default clients
                ClienteEntity c1 = new ClienteEntity(); c1.razonSocial = "Planta Industrial A";
                ClienteEntity c2 = new ClienteEntity(); c2.razonSocial = "Sede Logística B";
                ClienteEntity c3 = new ClienteEntity(); c3.razonSocial = "Fábrica Textil C";
                dao.insertClientes(Arrays.asList(c1, c2, c3));

                // Insert default residues
                ResiduoEntity r1 = new ResiduoEntity(); r1.nombre = "Plásticos Peligrosos";
                ResiduoEntity r2 = new ResiduoEntity(); r2.nombre = "Metales Pesados";
                ResiduoEntity r3 = new ResiduoEntity(); r3.nombre = "Cartón Industrial";
                ResiduoEntity r4 = new ResiduoEntity(); r4.nombre = "Químicos";
                dao.insertResiduos(Arrays.asList(r1, r2, r3, r4));
            });
        }
    };
}
