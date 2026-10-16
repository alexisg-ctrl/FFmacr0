
package com.ffmacro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class AutoTapService extends AccessibilityService {

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

    private long lastMacroTime = 0;
    private boolean macroRunning = false;

    private WindowManager wm;
    private View overlayView;
    private WindowManager.LayoutParams overlayParams;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        showOverlayButton();
    }

    private void showOverlayButton() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (overlayView != null) return;

        overlayView = new View(this);
        overlayView.setBackgroundColor(0xCCFFD700);

        overlayParams = new WindowManager.LayoutParams(
                80, 80,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        overlayParams.gravity = Gravity.TOP | Gravity.END;
        overlayParams.x = 10;
        overlayParams.y = 300;

        overlayView.setOnClickListener(v -> {
            long now = System.currentTimeMillis();

            if (macroRunning ||
                    now - lastMacroTime < COOLDOWN_MS) {
                return;
            }

            lastMacroTime = now;
            runMacro();
        });

        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int lastX, lastY, initX, initY;
            private boolean isDrag;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) e.getRawX();
                        lastY = (int) e.getRawY();
                        initX = overlayParams.x;
                        initY = overlayParams.y;
                        isDrag = false;
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) e.getRawX() - lastX;
                        int dy = (int) e.getRawY() - lastY;

                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            isDrag = true;
                        }

                        if (isDrag) {
                            overlayParams.x = initX - dx;
                            overlayParams.y = initY + dy;
                            wm.updateViewLayout(
                                    overlayView, overlayParams);
                        }
                        return isDrag;

                    default:
                        return false;
                }
            }
        });

        wm.addView(overlayView, overlayParams);
    }

    private void runMacro() {
        if (macroRunning) return;
        macroRunning = true;

        // 1. Abrir la tienda.
        tapAndWait(ABRIR_X, ABRIR_Y, success -> {
            if (!success) {
                finishMacro();
                return;
            }

            handler.postDelayed(() -> {
                // 2. Seleccionar la primera casilla.
                tapAndWait(M7_X, M7_Y, secondSuccess -> {
                    if (!secondSuccess) {
                        finishMacro();
                        return;
                    }

                    handler.postDelayed(() -> {
                        // 3. Confirmar la compra.
                        tapAndWait(
                                OBTENER_X,
                                OBTENER_Y,
                                thirdSuccess -> finishMacro()
                        );
                    }, ESPERA_CONFIRMAR_MS);

                });
            }, ESPERA_SELECCION_MS);
        });
    }

    private interface GestureResult {
        void onResult(boolean success);
    }

    private void tapAndWait(
            float x, float y, GestureResult callback) {

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(
                                new GestureDescription.StrokeDescription(
                                        path, 0, 60))
                        .build();

        boolean dispatched = dispatchGesture(
                gesture,
                new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription g) {
                        callback.onResult(true);
                    }

                    @Override
                    public void onCancelled(GestureDescription g) {
                        callback.onResult(false);
                    }
                },
                null
        );

        if (!dispatched) {
            callback.onResult(false);
        }
    }

    private void finishMacro() {
        macroRunning = false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {

            long now = System.currentTimeMillis();

            if (!macroRunning
                    && now - lastMacroTime >= COOLDOWN_MS) {
                lastMacroTime = now;
                runMacro();
            }

            return true;
        }

        return super.onKeyEvent(event);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Sin eventos adicionales.
    }

    @Override
    public void onInterrupt() {
        finishMacro();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (overlayView != null && wm != null) {
            try {
                wm.removeView(overlayView);
            } catch (Exception ignored) {
                // La vista ya podría haberse retirado.
            }
            overlayView = null;
        }

        super.onDestroy();
    }
}
