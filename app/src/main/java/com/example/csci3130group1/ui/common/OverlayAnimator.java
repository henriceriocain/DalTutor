package com.example.csci3130group1.ui.common;

import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

/**
 * Overlay-driven, flicker-proof transitions.
 * - Expand/collapse: Fades an overlay in on press, resizes under cover, then fades overlay out.
 * - Swap: Fades overlay in, runs provided content change under cover with ChangeBounds, then fades out.
 */
public final class OverlayAnimator {
    private OverlayAnimator() {}

    public interface BoolGetter { boolean get(); }
    public interface BoolSetter { void set(boolean value); }

    // Tuned timings for calm feel
    private static final long FADE_IN_OVERLAY_MS = 180;
    private static final long BOUNDS_MS = 240;
    private static final long FADE_OUT_DELAY_MS = 120;
    private static final long FADE_OUT_OVERLAY_MS = 300;

    private static final AccelerateDecelerateInterpolator EASE = new AccelerateDecelerateInterpolator();

    /**
     * Attach expand/collapse behavior to a header.
     * overlay must be a sibling above content that covers the card.
     */
    public static void attachExpandCollapse(View header,
                                            View overlay,
                                            ViewGroup card,
                                            View content,
                                            ImageView chevron,
                                            BoolGetter isExpanded,
                                            BoolSetter setExpanded) {
        // Apply initial state without animation
        boolean expanded = isExpanded != null && isExpanded.get();
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (chevron != null) chevron.setRotation(expanded ? 180f : 0f);
        header.setContentDescription(expanded ? "Collapse filters" : "Expand filters");

        header.setOnTouchListener(new View.OnTouchListener() {
            boolean animating = false;
            Boolean pendingTarget = null;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        if (animating) return true;
                        boolean current = isExpanded != null && isExpanded.get();
                        pendingTarget = !current;
                        animating = true;
                        overlay.animate().cancel();
                        overlay.setClickable(true);
                        overlay.setAlpha(0f);
                        overlay.animate().alpha(1f)
                                .setDuration(FADE_IN_OVERLAY_MS)
                                .setInterpolator(EASE)
                                .start();
                        return true;
                    }
                    case MotionEvent.ACTION_UP: {
                        if (!animating) return true;
                        boolean target = pendingTarget != null ? pendingTarget : !(isExpanded != null && isExpanded.get());
                        overlay.animate().cancel();
                        overlay.setAlpha(1f); // ensure fully opaque before content swap

                        ChangeBounds cb = new ChangeBounds();
                        cb.setDuration(BOUNDS_MS);
                        cb.setInterpolator(EASE);
                        cb.addListener(new Transition.TransitionListener() {
                            @Override public void onTransitionStart(Transition transition) {}
                            @Override public void onTransitionCancel(Transition transition) {}
                            @Override public void onTransitionPause(Transition transition) {}
                            @Override public void onTransitionResume(Transition transition) {}
                            @Override public void onTransitionEnd(Transition transition) {
                                overlay.animate().cancel();
                                overlay.animate().alpha(0f)
                                        .setStartDelay(FADE_OUT_DELAY_MS)
                                        .setDuration(FADE_OUT_OVERLAY_MS)
                                        .setInterpolator(EASE)
                                        .withEndAction(() -> {
                                            overlay.setClickable(false);
                                            animating = false;
                                            pendingTarget = null;
                                        })
                                        .start();
                            }
                        });
                        TransitionManager.beginDelayedTransition(card, cb);
                        content.setVisibility(target ? View.VISIBLE : View.GONE);
                        if (chevron != null) chevron.animate().rotation(target ? 180f : 0f).setDuration(200).setInterpolator(EASE).start();
                        header.setContentDescription(target ? "Collapse filters" : "Expand filters");
                        if (setExpanded != null) setExpanded.set(target);
                        return true;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        overlay.animate().cancel();
                        overlay.setAlpha(0f);
                        overlay.setClickable(false);
                        animating = false;
                        pendingTarget = null;
                        return true;
                    }
                }
                return true;
            }
        });
    }

    /**
     * Wraps a content change in a flicker-proof overlay + bounds transition.
     */
    public static void runSwap(View overlay, ViewGroup card, Runnable contentChangeAction) {
        if (overlay == null || card == null || contentChangeAction == null) return;

        overlay.animate().cancel();
        overlay.setClickable(true);
        overlay.setAlpha(0f);
        overlay.animate().alpha(1f)
                .setDuration(FADE_IN_OVERLAY_MS)
                .setInterpolator(EASE)
                .withEndAction(() -> {
                    ChangeBounds cb = new ChangeBounds();
                    cb.setDuration(BOUNDS_MS);
                    cb.setInterpolator(EASE);
                    cb.addListener(new Transition.TransitionListener() {
                        @Override public void onTransitionStart(Transition transition) {}
                        @Override public void onTransitionCancel(Transition transition) {}
                        @Override public void onTransitionPause(Transition transition) {}
                        @Override public void onTransitionResume(Transition transition) {}
                        @Override public void onTransitionEnd(Transition transition) {
                            overlay.animate().cancel();
                            overlay.animate().alpha(0f)
                                    .setStartDelay(FADE_OUT_DELAY_MS)
                                    .setDuration(FADE_OUT_OVERLAY_MS)
                                    .setInterpolator(EASE)
                                    .withEndAction(() -> overlay.setClickable(false))
                                    .start();
                        }
                    });
                    TransitionManager.beginDelayedTransition(card, cb);
                    contentChangeAction.run();
                })
                .start();
    }
}

