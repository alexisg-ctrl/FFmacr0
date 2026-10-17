package com.ffmacro;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/**
 * CalibrarActivity — Picker visual de coordenadas
 *
 * Flujo:
 * 1) Elige screenshot del juego desde galería
 * 2) Toca el botón en la imagen → guarda coord en prefs
 * 3) Repite para TAP 1, 2 y 3
 * 4) AutoTapService lee las coords guardadas en prefs
 */
public class CalibrarActivity extends AppCompatActivity {

    public static final String PREFS = "ffmacro_coords";
    public static final String K_T1X = "t1x";
    public static final String K_T1Y = "t1y";
    public static final String K_T2X = "t2x";
    public static final String K_T2Y = "t2y";
    public static final String K_T3X = "t3x";
    public static final String K_T3Y = "t3y";

    private ImageView imgPreview;
    private TextView tvInstruccion;
    private Button btnSelImg, btnSiguiente, btnSaltar;
    private Bitmap bitmapOriginal;
    private Bitmap bitmapConMarcas;
    private Paint paintMarca;

    private int paso = 0; // 0=T1, 1=T2, 2=T3, 3=listo
    private float[] guardados = new float[6]; // t1x,t1y,t2x,t2y,t3x,t3y

    private final String[] INSTRUCCIONES = {
        "📸 Paso 1/3 — TAP 1\nToca donde está el botón 🛒 COMPRAR del HUD\n(el que abre la tienda)",
        "📸 Paso 2/3 — TAP 2\nToca donde está SUPERRESURRECCIÓN\n(casilla top-izquierda en REVIVIR)",
        "📸 Paso 3/3 — TAP 3\nToca donde está el botón COMPRAR amarillo\n(bottom-right del modal)"
    };

    private final ActivityResultLauncher<String> pickerLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) cargarImagen(uri);
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrar);

        imgPreview    = findViewById(R.id.imgPreview);
        tvInstruccion = findViewById(R.id.tvInstruccion);
        btnSelImg     = findViewById(R.id.btnSelImg);
        btnSiguiente  = findViewById(R.id.btnSiguiente);
        btnSaltar     = findViewById(R.id.btnSaltar);

        paintMarca = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMarca.setColor(Color.RED);
        paintMarca.setStyle(Paint.Style.FILL);
        paintMarca.setStrokeWidth(4);

        actualizarUI();

        btnSelImg.setOnClickListener(v -> pickerLauncher.launch("image/*"));

        btnSiguiente.setOnClickListener(v -> {
            paso++;
            bitmapOriginal = null;
            bitmapConMarcas = null;
            imgPreview.setImageResource(R.drawable.placeholder_calibrar);
            actualizarUI();
        });

        btnSaltar.setOnClickListener(v -> finish());

        imgPreview.setOnTouchListener((v, e) -> {
            if (bitmapOriginal == null) {
                Toast.makeText(this, "Primero carga un screenshot", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (e.getAction() != android.view.MotionEvent.ACTION_UP) return true;

            // Convertir toque en la ImageView → coords relativas al bitmap
            float viewW = v.getWidth();
            float viewH = v.getHeight();
            float bmpW  = bitmapOriginal.getWidth();
            float bmpH  = bitmapOriginal.getHeight();

            // ImageView en modo FIT_CENTER — calcular offset y escala
            float scaleX = viewW / bmpW;
            float scaleY = viewH / bmpH;
            float scale  = Math.min(scaleX, scaleY);
            float offsetX = (viewW - bmpW * scale) / 2f;
            float offsetY = (viewH - bmpH * scale) / 2f;

            float bmpTouchX = (e.getX() - offsetX) / scale;
            float bmpTouchY = (e.getY() - offsetY) / scale;

            // Clampar dentro del bitmap
            bmpTouchX = Math.max(0, Math.min(bmpW - 1, bmpTouchX));
            bmpTouchY = Math.max(0, Math.min(bmpH - 1, bmpTouchY));

            float ratioX = bmpTouchX / bmpW;
            float ratioY = bmpTouchY / bmpH;

            guardarCoord(ratioX, ratioY);
            dibujarMarca(bmpTouchX, bmpTouchY, scale, offsetX, offsetY);
            return true;
        });
    }

    private void cargarImagen(Uri uri) {
        try {
            bitmapOriginal = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            bitmapConMarcas = bitmapOriginal.copy(Bitmap.Config.ARGB_8888, true);
            imgPreview.setImageBitmap(bitmapConMarcas);
            tvInstruccion.setText(INSTRUCCIONES[paso] + "\n\n✅ Imagen cargada — ahora toca el botón");
        } catch (Exception ex) {
            Toast.makeText(this, "Error cargando imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarCoord(float rx, float ry) {
        int idx = paso * 2;
        guardados[idx]     = rx;
        guardados[idx + 1] = ry;

        // Guardar en SharedPreferences
        SharedPreferences.Editor ed = getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        String[] keys = {K_T1X, K_T1Y, K_T2X, K_T2Y, K_T3X, K_T3Y};
        ed.putFloat(keys[idx],     rx);
        ed.putFloat(keys[idx + 1], ry);
        ed.apply();

        String msg = String.format("✅ TAP %d guardado\nX=%.3f  Y=%.3f\n\nToca «Siguiente» para continuar", paso + 1, rx, ry);
        tvInstruccion.setText(msg);
        btnSiguiente.setEnabled(true);
        btnSiguiente.setAlpha(1f);

        if (paso == 2) {
            btnSiguiente.setText("✅ GUARDAR Y SALIR");
            btnSiguiente.setOnClickListener(v -> {
                Toast.makeText(this, "¡Coordenadas guardadas! El macro usará estas posiciones.", Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    private void dibujarMarca(float bx, float by, float scale, float ox, float oy) {
        if (bitmapConMarcas == null) return;
        Canvas c = new Canvas(bitmapConMarcas);
        // Cruz roja en la posición tocada
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.RED);
        p.setStrokeWidth(6);
        c.drawCircle(bx, by, 20, p);
        p.setColor(Color.WHITE);
        p.setStrokeWidth(3);
        c.drawLine(bx - 30, by, bx + 30, by, p);
        c.drawLine(bx, by - 30, bx, by + 30, p);
        imgPreview.setImageBitmap(bitmapConMarcas);
    }

    private void actualizarUI() {
        if (paso < 3) {
            tvInstruccion.setText(INSTRUCCIONES[paso] + "\n\n📂 Primero carga un screenshot del juego");
            btnSiguiente.setText("Siguiente →");
            btnSiguiente.setEnabled(false);
            btnSiguiente.setAlpha(0.4f);
        }
    }
}
