package com.example.android.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.example.android.api.ApiClient;
import com.example.android.data.local.EcolimDatabase;
import com.example.android.data.local.dao.EcolimDao;
import com.example.android.data.local.entity.UsuarioEntity;
import com.example.android.utils.SessionManager;
import com.example.android.MainActivity;
import com.example.android.R;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText etCorreo, etPassword;
    MaterialButton btnLogin;
    TextView tvRegister;
    EcolimDao dao;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navegarAlDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        dao = EcolimDatabase.getDatabase(this).ecolimDao();
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            String correo = etCorreo.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

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

            btnLogin.setEnabled(false);
            btnLogin.setText("Validando...");

            // Intentar login por API
            try {
                JSONObject payload = new JSONObject();
                payload.put("correo", correo);
                payload.put("password", password);

                ApiClient.loginUsuario(payload, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String responseBody) {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            int userId = json.optInt("id_usuario", 1);
                            String nombre = json.getString("usuario");
                            String rol = json.getString("rol");
                            
                            // Guardar en base local para offline
                            EcolimDatabase.databaseWriteExecutor.execute(() -> {
                                UsuarioEntity user = new UsuarioEntity();
                                user.idUsuario = userId;
                                user.nombre = nombre;
                                user.correo = correo;
                                user.password = password;
                                user.rol = rol;
                                dao.insertUsuario(user);
                            });

                            runOnUiThread(() -> {
                                sessionManager.crearSesion(userId, nombre, correo, rol);
                                Toast.makeText(LoginActivity.this, "Bienvenido, " + nombre, Toast.LENGTH_SHORT).show();
                                navegarAlDashboard();
                            });
                        } catch (Exception e) {
                            fallBackLocal(correo, password);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e("LOGIN", "Error API: " + errorMessage);
                        fallBackLocal(correo, password);
                    }
                });
            } catch (Exception e) {
                fallBackLocal(correo, password);
            }
        });
    }

    private void fallBackLocal(String correo, String password) {
        EcolimDatabase.databaseWriteExecutor.execute(() -> {
            UsuarioEntity user = dao.login(correo, password);
            runOnUiThread(() -> {
                btnLogin.setEnabled(true);
                btnLogin.setText("Ingresar");
                
                if (user != null) {
                    sessionManager.crearSesion(user.idUsuario, user.nombre, user.correo, user.rol != null ? user.rol : "OPERARIO");
                    Toast.makeText(this, "Bienvenido (Modo Offline), " + user.nombre, Toast.LENGTH_SHORT).show();
                    navegarAlDashboard();
                } else {
                    Toast.makeText(this, "Credenciales incorrectas o sin conexión", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void navegarAlDashboard() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
