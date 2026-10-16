package com.ffmacro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

public class AutoTapService extends AccessibilityService {

    private WindowManager wm;
    private TextView boton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean probando = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        crearBoton();
    }

    private void crearBoton() {
        if (boton != null) return;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        boton = new TextView(this);
        boton.setText("PROBAR");
        boton.setTextColor(Color.WHITE);
        boton.setTextSize(14);
        boton.setGravity(Gravity.CENTER);
        boton.setBackground(fondo(Color.RED));

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            110, 90,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = 20;
        p.y = 300;

        boton.setOnClickListener(v -> probarGesto());
        wm.addView(boton, p);
    }

    private GradientDrawable fondo(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(20);
        d.setColor(color);
        return d;
    }

    private void probarGesto() {
        if (probando) return;
        probando = true;
        boton.setText("PROBANDO");
        boton.setBackground(fondo(Color.rgb(190, 130, 0)));

        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);

        float x = dm.widthPixels / 2f;
        float y = dm.heightPixels / 2f;

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 100))
            .build();

        boolean enviado = dispatchGesture(
            gesture,
            new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription g) {
                    mostrarResultado("ACEPTADO", Color.rgb(0, 140, 0));
                }

                @Override
                public void onCancelled(GestureDescription g) {
                    mostrarResultado("CANCELADO", Color.RED);
                }
            },
            null
        );

        if (!enviado) {
            mostrarResultado("RECHAZADO", Color.RED);
        }
    }

    private void mostrarResultado(String texto, int color) {
        probando = false;
        boton.setText(texto);
        boton.setBackground(fondo(color));

        handler.postDelayed(() -> {
            if (boton != null && !probando) {
                boton.setText("PROBAR");
                boton.setBackground(fondo(Color.RED));
            }
        }, 2500);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        probando = false;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (boton != null && wm != null) {
            try {
                wm.removeView(boton);
            } catch (Exception ignored) {
            }
            boton = null;
        }
        super.onDestroy();
    }
}
