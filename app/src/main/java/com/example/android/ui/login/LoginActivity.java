package com.example.android.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.example.android.data.database.DatabaseHelper;
import com.example.android.utils.SessionManager;
import com.example.android.MainActivity;
import com.example.android.R;

/**
 * LoginActivity — Pantalla de autenticación del operario.
 * Es la Activity LAUNCHER (primera pantalla al abrir la app).
 * Valida credenciales contra SQLite (offline) y guarda sesión en SharedPreferences.
 */
public class LoginActivity extends AppCompatActivity {

    TextInputEditText etCorreo, etPassword;
    MaterialButton btnLogin;
    DatabaseHelper dbHelper;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar si ya hay sesión activa (evitar login repetitivo)
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navegarAlDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        // Inicializar componentes
        dbHelper = new DatabaseHelper(this);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Listener del botón de login
        btnLogin.setOnClickListener(v -> {
            String correo = etCorreo.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validar campos vacíos
            if (correo.isEmpty()) {
                etCorreo.setError("Ingrese su correo corporativo");
                etCorreo.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Ingrese su contraseña");
                etPassword.requestFocus();
                return;
            }

            // Validar credenciales contra SQLite
            if (dbHelper.login(correo, password)) {
                // Login EXITOSO: Guardar sesión
                int userId = dbHelper.getUsuarioId(correo);
                String nombre = dbHelper.getNombreUsuario(correo);
                sessionManager.crearSesion(userId, nombre, correo, "OPERARIO");

                Toast.makeText(this, "Bienvenido, " + nombre,
                        Toast.LENGTH_SHORT).show();
                navegarAlDashboard();
            } else {
                // Login FALLIDO
                Toast.makeText(this,
                        "Credenciales incorrectas\n(Usa: admin@ecolim.com / 123456)",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Navega al Dashboard y cierra LoginActivity.
     */
    private void navegarAlDashboard() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish(); // Impedir retroceso al login
    }
}
