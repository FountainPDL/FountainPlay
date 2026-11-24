package com.fountainpdl.fountainplay.util;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/** Small, cheap animations that make the UI feel alive. */
public final class Fx {
    private Fx() {}

    /** Squish-on-press feedback. Does not consume the touch, so clicks still work. */
    @SuppressLint("ClickableViewAccessibility")
    public static void press(View v) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().setStartDelay(0).scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().setStartDelay(0).scaleX(1f).scaleY(1f).setDuration(220)
                        .setInterpolator(new OvershootInterpolator(2.2f)).start();
                    break;
                default: break;
            }
            return false;
        });
    }

    /** Slide-up + fade-in, staggered by index. */
    public static void enter(View v, int index) {
        v.animate().cancel();
        v.setAlpha(0f);
        v.setTranslationY(dp(v, 20));
        v.animate().alpha(1f).translationY(0f)
            .setStartDelay(Math.min(Math.max(index, 0), 12) * 45L)
            .setDuration(340).setInterpolator(new DecelerateInterpolator(1.7f)).start();
    }

    /** Resets a recycled view so a cancelled entrance animation cannot leave it invisible. */
    public static void reset(View v) {
        v.animate().cancel();
        v.setAlpha(1f);
        v.setTranslationY(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
    }

    public static void enterChildren(ViewGroup g) {
        int shown = 0;
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c.getVisibility() == View.VISIBLE) enter(c, shown++);
        }
    }

    /** Heart-style pop. */
    public static void pop(View v) {
        v.animate().cancel();
        v.setScaleX(0.6f);
        v.setScaleY(0.6f);
        v.animate().setStartDelay(0).scaleX(1f).scaleY(1f).setDuration(380)
            .setInterpolator(new OvershootInterpolator(3f)).start();
    }

    private static float dp(View v, int dp) { return dp * v.getResources().getDisplayMetrics().density; }
}
