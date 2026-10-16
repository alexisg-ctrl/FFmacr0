package com.ffmacro;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = findViewById(R.id.tvStatus);
        Button btn = findViewById(R.id.btnEnable);
        btn.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvStatus.setText(isServiceEnabled()
            ? "⬤  ACTIVO — Toca el botón flotante"
            : "⬤  Servicio desactivado");
    }

    private boolean isServiceEnabled() {
        try {
            String s = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return s != null && s.contains(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }
}
