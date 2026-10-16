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

    private static final float ABRIR_X = 1120f;
    private static final float ABRIR_Y = 541f;

    private static final float M7_X = 402f;
    private static final float M7_Y = 399f;

    private static final float OBTENER_X = 1467f;
    private static final float OBTENER_Y = 892f;

    private static final long ESPERA_SELECCION_MS = 500;
    private static final long ESPERA_CONFIRMAR_MS = 400;
    private static final long TIMEOUT_GESTO_MS = 1500;
    private static final long COOLDOWN_MS = 1000;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private WindowManager wm;
    private TextView boton;
    private WindowManager.LayoutParams params;

    private boolean ejecutando = false;
    private long ultimoInicio = 0;

    private final int ROJO = Color.rgb(220, 30, 30);
    private final int VERDE = Color.rgb(30, 170, 70);

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

    private void actualizarBoton(int color, String texto) {
        if (boton != null) {
            boton.setBackground(fondo(color));
            boton.setText(texto);
        }
    }

    private void crearBoton() {
        if (boton != null) return;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        boton = new TextView(this);
        boton.setText("GO");
        boton.setTextColor(Color.WHITE);
        boton.setTextSize(18);
        boton.setGravity(Gravity.CENTER);
        boton.setBackground(fondo(ROJO));

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
        actualizarBoton(VERDE, "...");

        tocar(ABRIR_X, ABRIR_Y, ok1 -> {
            if (!ok1) {
                terminar("!");
                return;
            }

            handler.postDelayed(() -> {
                tocar(M7_X, M7_Y, ok2 -> {
                    if (!ok2) {
                        terminar("!");
                        return;
                    }

                    handler.postDelayed(() -> {
                        tocar(OBTENER_X, OBTENER_Y, ok3 -> {
                            terminar(ok3 ? "OK" : "!");
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
        final boolean[] respondido = {false};

        Runnable timeout = () -> {
            if (!respondido[0]) {
                respondido[0] = true;
                resultado.completar(false);
            }
        };

        handler.postDelayed(timeout, TIMEOUT_GESTO_MS);

        Path ruta = new Path();
        ruta.moveTo(x, y);

        GestureDescription gesto =
                new GestureDescription.Builder()
                        .addStroke(
                                new GestureDescription.StrokeDescription(
                                        ruta, 0, 100
                                )
                        )
                        .build();

        boolean enviado = dispatchGesture(
                gesto,
                new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription g) {
                        if (!respondido[0]) {
                            respondido[0] = true;
                            handler.removeCallbacks(timeout);
                            resultado.completar(true);
                        }
                    }

                    @Override
                    public void onCancelled(GestureDescription g) {
                        if (!respondido[0]) {
                            respondido[0] = true;
                            handler.removeCallbacks(timeout);
                            resultado.completar(false);
                        }
                    }
                },
                null
        );

        if (!enviado && !respondido[0]) {
            respondido[0] = true;
            handler.removeCallbacks(timeout);
            resultado.completar(false);
        }
    }

    private void terminar(String texto) {
        ejecutando = false;
        actualizarBoton(ROJO, texto);

        handler.postDelayed(() -> {
            if (!ejecutando) {
                actualizarBoton(ROJO, "GO");
            }
        }, 1000);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        ejecutando = false;
        actualizarBoton(ROJO, "GO");
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
