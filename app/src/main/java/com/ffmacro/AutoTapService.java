package com.ffmacro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * AutoTapService — Free Fire MAX Shop Macro
 *
 * Secuencia de 3 taps FULL AUTO — para mishero que necesita revivir rápido:
 *   TAP 1 → Botón COMPRAR del HUD (abre la tienda)
 *   TAP 2 → Superresurrección (sección REVIVIR, casilla izquierda)
 *   TAP 3 → Botón COMPRAR del modal (confirma)
 *
 * Solo presiona el botón flotante — hace todo solo.
 * Calibrado para pantalla PORTRAIT 2412x1080.
 */
public class AutoTapService extends AccessibilityService {

    // ─── Timing ───────────────────────────────────────────────────────────────
    private static final long DURACION_TAP   = 80;    // ms duración de cada tap
    private static final long DELAY_T1_a_T2  = 750;   // ms entre tap1 (abrir tienda) y tap2 (selección)
    private static final long DELAY_T2_a_T3  = 300;   // ms entre tap2 (selección) y tap3 (confirmar)
    private static final long COOLDOWN       = 1200;   // ms tras completar secuencia

    // ─── Coordenadas — portrait 2412x1080 (calibradas de screenshots reales) ─
    //
    // TAP 1: Botón COMPRAR del HUD (carrito, abre tienda)
    // x=710/1080=0.657  y=415/2412=0.172
    private static final float T1_X = 0.657f;
    private static final float T1_Y = 0.172f;

    // TAP 2: SUPERRESURRECCIÓN — sección REVIVIR, casilla izquierda
    // x=277/1080=0.256  y=200/2412=0.083
    private static final float T2_X = 0.256f;
    private static final float T2_Y = 0.083f;

    // TAP 3: Botón COMPRAR del modal — esquina inferior derecha
    // x=905/1080=0.838  y=597/2412=0.248
    private static final float T3_X = 0.838f;
    private static final float T3_Y = 0.248f;

    // ─── Estado ───────────────────────────────────────────────────────────────
    private WindowManager wm;
    private View overlayRoot;
    private TextView btnLabel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ejecutando = false;
    private int baseX, baseY;
    private float rawX0, rawY0;

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        crearOverlay();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) { }
    @Override public void onInterrupt() { ejecutando = false; }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (overlayRoot != null && wm != null) {
            try { wm.removeView(overlayRoot); } catch (Exception ignored) { }
            overlayRoot = null;
        }
        super.onDestroy();
    }

    // ─── Crear botón flotante ─────────────────────────────────────────────────
    private void crearOverlay() {
        if (overlayRoot != null) return;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        btnLabel = new TextView(this);
        btnLabel.setText("🛒\nFF BUY");
        btnLabel.setTextColor(Color.WHITE);
        btnLabel.setTextSize(11f);
        btnLabel.setGravity(Gravity.CENTER);
        btnLabel.setPadding(14, 10, 14, 10);
        btnLabel.setBackground(bg(Color.parseColor("#CC1565C0"), 20));
        root.addView(btnLabel);

        overlayRoot = root;

        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            140, 96,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 24;
        lp.y = 420;

        overlayRoot.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    baseX = lp.x; baseY = lp.y;
                    rawX0 = e.getRawX(); rawY0 = e.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int dx = (int)(e.getRawX() - rawX0);
                    int dy = (int)(e.getRawY() - rawY0);
                    if (Math.abs(dx) > 6 || Math.abs(dy) > 6) {
                        lp.x = baseX + dx;
                        lp.y = baseY + dy;
                        wm.updateViewLayout(overlayRoot, lp);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    int mX = (int)(e.getRawX() - rawX0);
                    int mY = (int)(e.getRawY() - rawY0);
                    if (Math.abs(mX) < 12 && Math.abs(mY) < 12) {
                        // Tap corto → lanzar macro
                        ejecutarSecuencia();
                    }
                    return true;
            }
            return false;
        });

        wm.addView(overlayRoot, lp);
    }

    // ─── Secuencia de 3 taps FULL AUTO ───────────────────────────────────────
    private void ejecutarSecuencia() {
        if (ejecutando) return;
        ejecutando = true;
        setUI("⏳ 1/3", Color.parseColor("#CCEF6C00"));

        // Timeout de seguridad: si en 4s no terminó, resetea solo
        handler.postDelayed(() -> {
            if (ejecutando) resetUI();
        }, 4000);

        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        final float W = dm.widthPixels;
        final float H = dm.heightPixels;

        // TAP 1 — Abre tienda (botón COMPRAR del HUD)
        tap(W * T1_X, H * T1_Y, () -> {
            setUI("⏳ 2/3", Color.parseColor("#CCEF6C00"));

            // TAP 2 — Superresurrección (espera que cargue el modal)
            handler.postDelayed(() ->
                tap(W * T2_X, H * T2_Y, () -> {
                    setUI("⏳ 3/3", Color.parseColor("#CC43A047"));

                    // TAP 3 — COMPRAR confirmar
                    handler.postDelayed(() ->
                        tap(W * T3_X, H * T3_Y, () -> {
                            setUI("✅ LISTO", Color.parseColor("#CC43A047"));
                            handler.postDelayed(this::resetUI, COOLDOWN);
                        }),
                    DELAY_T2_a_T3);
                }),
            DELAY_T1_a_T2);
        });
    }

    // ─── Tap con callback ─────────────────────────────────────────────────────
    private void tap(float x, float y, Runnable onDone) {
        // Offset aleatorio ±3px (anti-detección)
        float rx = x + (float)(Math.random() * 6 - 3);
        float ry = y + (float)(Math.random() * 6 - 3);

        Path p = new Path();
        p.moveTo(rx, ry);

        GestureDescription g = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, DURACION_TAP))
            .build();

        dispatchGesture(g, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gd) {
                if (onDone != null) handler.post(onDone);
            }
            @Override
            public void onCancelled(GestureDescription gd) {
                setUI("❌ ERROR", Color.parseColor("#CCDD2C00"));
                handler.postDelayed(AutoTapService.this::resetUI, 1500);
            }
        }, null);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private void setUI(String txt, int color) {
        handler.post(() -> {
            if (btnLabel != null) {
                btnLabel.setText(txt);
                btnLabel.setBackground(bg(color, 20));
            }
        });
    }

    private void resetUI() {
        ejecutando = false;
        setUI("🛒\nFF BUY", Color.parseColor("#CC1565C0"));
    }

    private GradientDrawable bg(int color, float r) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(r);
        d.setColor(color);
        return d;
    }
}
