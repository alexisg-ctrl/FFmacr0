package com.ffmacro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import java.util.Random;

public class AutoTapService extends AccessibilityService {

    private static final float ABRIR_X   = 1120f;
    private static final float ABRIR_Y   = 541f;
    private static final float M7_X      = 402f;
    private static final float M7_Y      = 399f;
    private static final float OBTENER_X = 1467f;
    private static final float OBTENER_Y = 892f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rng = new Random();
    private long lastMacroTime = 0;
    private static final long COOLDOWN_MS = 800;

    private WindowManager wm;
    private View overlayView;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        showOverlayButton();
    }

    private void showOverlayButton() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = new View(this);
        overlayView.setBackgroundColor(0xCCFFD700);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            80, 80,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 10;
        params.y = 300;

        overlayView.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastMacroTime < COOLDOWN_MS) return;
            lastMacroTime = now;
            runMacro();
        });

        overlayView.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY, initX, initY;
            boolean isDrag;
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) e.getRawX();
                        lastY = (int) e.getRawY();
                        initX = params.x;
                        initY = params.y;
                        isDrag = false;
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) e.getRawX() - lastX;
                        int dy = (int) e.getRawY() - lastY;
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) isDrag = true;
                        if (isDrag) {
                            params.x = initX - dx;
                            params.y = initY + dy;
                            wm.updateViewLayout(overlayView, params);
                        }
                        return isDrag;
                    default:
                        return false;
                }
            }
        });

        wm.addView(overlayView, params);
    }

    private void runMacro() {
        tap(ABRIR_X, ABRIR_Y);
        handler.postDelayed(() -> tap(M7_X, M7_Y), 70 + rng.nextInt(40));
        handler.postDelayed(() -> tap(OBTENER_X, OBTENER_Y), 150 + rng.nextInt(60));
    }

    private void tap(float baseX, float baseY) {
        float x = baseX + (rng.nextFloat() * 6f) - 3f;
        float y = baseY + (rng.nextFloat() * 6f) - 3f;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription g = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 45 + rng.nextInt(30)))
            .build();
        dispatchGesture(g, null, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) wm.removeView(overlayView);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
