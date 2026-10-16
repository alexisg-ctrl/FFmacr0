
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
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

public class AutoTapService extends AccessibilityService {

    // Tus coordenadas originales
    private static final float ABRIR_X = 1120f;
    private static final float ABRIR_Y = 541f;

    private static final float M7_X = 402f;
    private static final float M7_Y = 399f;

    private static final float OBTENER_X = 1467f;
    private static final float OBTENER_Y = 892f;

    private static final long ESPERA_SELECCION_MS = 300;
    private static final long ESPERA_CONFIRMAR_MS = 250;
    private static final long COOLDOWN_MS = 800;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private WindowManager wm;
    private TextView boton;
    private WindowManager.LayoutParams params;

    private boolean ejecutando = false;
    private long ultimoInicio = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        crearBoton();
    }

    private GradientDrawable fondo(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(4, Color.WHITE);
        return drawable;
    }

    private void actualizarColor(int color, String texto) {
        if (boton == null) return;

        boton.setBackground(fondo(color));
        boton.setText(texto);
    }

    private void crearBoton() {
        if (boton != null) return;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        boton = new TextView(this);
        boton.setText("GO");
        boton.setTextColor(Color.WHITE);
        boton.setTextSize(18);
        boton.setGravity(Gravity.CENTER);
        boton.setElevation(12);
        boton.setBackground(fondo(Color.rgb(220, 30, 30)));
        boton.setContentDescription("Ejecutar las tres acciones");

        params = new WindowManager.LayoutParams(
                90,
                90,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 25;
        params.y = 350;

        boton.setOnClickListener(v -> {
            long ahora = System.currentTimeMillis();

            if (ejecutando ||
                    ahora - ultimoInicio < COOLDOWN_MS) {
                return;
            }

            ultimoInicio = ahora;
            ejecutarMacro();
        });

        wm.addView(boton, params);
    }

    private void ejecutarMacro() {
        if (ejecutando) return;

        ejecutando = true;
        actualizarColor(Color.rgb(30, 170, 70), "...");

        // Acción 1: abrir
        tocar(ABRIR_X, ABRIR_Y, ok1 -> {
            if (!ok1) {
                terminar();
                return;
            }

            handler.postDelayed(() -> {
                // Acción 2: seleccionar
                tocar(M7_X, M7_Y, ok2 -> {
                    if (!ok2) {
                        terminar();
                        return;
                    }

                    handler.postDelayed(() -> {
                        // Acción 3: obtener / confirmar
                        tocar(OBTENER_X, OBTENER_Y, ok3 -> {
                            terminar();
                        });
                    }, ESPERA_CONFIRMAR_MS);

                });
            }, ESPERA_SELECCION_MS);
        });
    }

    private interface Resultado {
        void completar(boolean correcto);
    }

    private void tocar(float x, float y, Resultado resultado) {
        Path ruta = new Path();
        ruta.moveTo(x, y);

        GestureDescription gesto =
                new GestureDescription.Builder()
                        .addStroke(
                                new GestureDescription.StrokeDescription(
                                        ruta, 0, 60
                                )
                        )
                        .build();

        boolean enviado = dispatchGesture(
                gesto,
                new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription g) {
                        resultado.completar(true);
                    }

                    @Override
                    public void onCancelled(GestureDescription g) {
                        resultado.completar(false);
                    }
                },
                null
        );

        if (!enviado) {
            resultado.completar(false);
        }
    }

    private void terminar() {
        ejecutando = false;
        actualizarColor(Color.rgb(220, 30, 30), "GO");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No necesitamos procesar eventos para este botón.
    }

    @Override
    public void onInterrupt() {
        terminar();
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
