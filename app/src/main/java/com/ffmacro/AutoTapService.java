package com.ffmacro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import java.util.Random;

public class AutoTapService extends AccessibilityService {

    // Coordenadas base
    private static final float ABRIR_X   = 718f;
    private static final float ABRIR_Y   = 370f;
    private static final float M7_X      = 255f;
    private static final float M7_Y      = 255f;
    private static final float OBTENER_X = 968f;
    private static final float OBTENER_Y = 581f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rng = new Random();

    // Anti-cheat: evita spam de vol+ repetido
    private long lastMacroTime = 0;
    private static final long COOLDOWN_MS = 800;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                && event.getAction() == KeyEvent.ACTION_DOWN) {

            long now = System.currentTimeMillis();
            if (now - lastMacroTime < COOLDOWN_MS) return true; // ignora si muy rápido
            lastMacroTime = now;

            runMacro();
            return true;
        }
        return super.onKeyEvent(event);
    }

    private void runMacro() {
        // TAP 1 — ABRIR (inmediato + micro offset aleatorio)
        tap(ABRIR_X, ABRIR_Y);

        // TAP 2 — M7 (delay humano aleatorio entre 70-110ms)
        long delay2 = 70 + rng.nextInt(40);
        handler.postDelayed(() -> tap(M7_X, M7_Y), delay2);

        // TAP 3 — OBTENER (delay humano aleatorio entre 140-200ms)
        long delay3 = 140 + rng.nextInt(60);
        handler.postDelayed(() -> tap(OBTENER_X, OBTENER_Y), delay3);
    }

    private void tap(float baseX, float baseY) {
        // Micro offset aleatorio ±3px — imita dedo humano impreciso
        float x = baseX + (rng.nextFloat() * 6f) - 3f;
        float y = baseY + (rng.nextFloat() * 6f) - 3f;

        // Duración del tap aleatoria entre 45-75ms
        int duration = 45 + rng.nextInt(30);

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, duration))
            .build();

        dispatchGesture(gesture, null, null);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
