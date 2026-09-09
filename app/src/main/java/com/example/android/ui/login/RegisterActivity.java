package com.example.android.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android.MainActivity;
import com.example.android.R;
import com.example.android.api.ApiClient;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etApellido, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        tvLogin.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        try {
            JSONObject json = new JSONObject();
            json.put("nombre", nombre);
            json.put("apellido", apellido);
            json.put("correo", email);
            json.put("password", password);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    json.toString()
            );

            // Using the new OkHttp POST method
            ApiClient.registrarUsuario(json, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String responseBody) {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "¡Registro exitoso! Por favor inicia sesión.", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Log.e("Register", "Error: " + errorMessage);
                    Toast.makeText(RegisterActivity.this, "Error de registro: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
        }
    }
}
