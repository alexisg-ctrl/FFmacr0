package com.ffmacro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class AutoTapService extends AccessibilityService {

    private static final float ABRIR_X   = 718f;
    private static final float ABRIR_Y   = 370f;
    private static final float M7_X      = 255f;
    private static final float M7_Y      = 255f;
    private static final float OBTENER_X = 968f;
    private static final float OBTENER_Y = 581f;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            runMacro();
            return true;
        }
        return super.onKeyEvent(event);
    }

    private void runMacro() {
        tap(ABRIR_X, ABRIR_Y);
        handler.postDelayed(() -> tap(M7_X, M7_Y), 200);
        handler.postDelayed(() -> tap(OBTENER_X, OBTENER_Y), 420);
    }

    private void tap(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription g = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 60))
            .build();
        dispatchGesture(g, null, null);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
