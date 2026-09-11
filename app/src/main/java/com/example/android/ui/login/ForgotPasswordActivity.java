package com.example.android.ui.login;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android.R;
import com.example.android.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etCorreo;
    private MaterialButton btnRecuperar;
    private TextView tvVolverLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etCorreo = findViewById(R.id.etCorreo);
        btnRecuperar = findViewById(R.id.btnRecuperar);
        tvVolverLogin = findViewById(R.id.tvVolverLogin);

        tvVolverLogin.setOnClickListener(v -> finish());

        btnRecuperar.setOnClickListener(v -> {
            String correo = etCorreo.getText().toString().trim();

            if (correo.isEmpty()) {
                etCorreo.setError("Ingrese su correo electrónico");
                etCorreo.requestFocus();
                return;
            }

            btnRecuperar.setEnabled(false);
            btnRecuperar.setText("Enviando...");

            ApiClient.recuperarPassword(correo, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        btnRecuperar.setEnabled(true);
                        btnRecuperar.setText("Enviar Correo");
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String message = json.optString("message", "Instrucciones enviadas.");
                            Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(ForgotPasswordActivity.this, "Instrucciones enviadas", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        btnRecuperar.setEnabled(true);
                        btnRecuperar.setText("Enviar Correo");
                        Log.e("FORGOT_PASSWORD", "Error API: " + errorMessage);
                        Toast.makeText(ForgotPasswordActivity.this, "Error de red, intenta nuevamente", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }
}
