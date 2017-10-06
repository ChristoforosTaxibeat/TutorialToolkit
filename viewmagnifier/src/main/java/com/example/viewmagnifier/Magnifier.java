package com.example.viewmagnifier;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by christoforoskolovos on 06/10/2017.
 */

public class Magnifier extends Dialog implements View.OnClickListener, View.OnTouchListener {
    Context context;
    View targetView;
    String text;
    int min_width, min_height, min_center_left, min_center_top, max_center_left, max_center_top,
            max_width, max_height, screen_width, screen_height;
    float min_scaleX, min_scaleY, max_scaleX, max_scaleY, min_translationX, min_translationY,
            max_translationX, max_translationY, max_background_alpha, min_background_alpha,
            max_textView_alpha, min_textView_alpha, dX, dY;
    boolean closeAnimationCompleted;
    boolean dismissGestureDetected;
    boolean instantDismissRequested;
    stateListener stateListener;

    ConstraintLayout mainContainer;
    View background;
    ImageView viewContainer;
    TextView textView;


    public Magnifier(Context context, View targetView, float scale, String text) {

        super(context, R.style.Transparent);

        this.context = context;
        this.targetView = targetView;
        this.text = text;
        max_scaleX = scale;
        max_scaleY = scale;

        initViews();

        setView();

        setText();

        viewContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                viewContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                setBasicMeasurementVariables();
                expand();
                return true;
            }
        });
    }

    private void setView() {
        final float originalScaleX = targetView.getScaleX();
        final float originalScaleY = targetView.getScaleY();

        targetView.setDrawingCacheEnabled(true);
        Bitmap bitmap = targetView.getDrawingCache();
        viewContainer.setImageBitmap(bitmap);

        targetView.setScaleX(originalScaleX);
        targetView.setScaleY(originalScaleY);
    }

    private void setText() {
        textView.setText(text);

//// TODO: 06/10/2017 set dynamic padding based on scale difference in order to be equal with 20dp top padding in normal scale
//        textView.setPadding(0, (int) ((ViewUtils.dpToPx(targetView.getResources(), 20) * min_scaleX)) / 2, 0, 0);
//        Log.i("Test", "Padding: " + (int) ((ViewUtils.dpToPx(targetView.getResources(), 20) * min_scaleX)) / 2);
    }

    private void initViews() {

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(layoutParams);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        this.setContentView(R.layout.magnifier);

        mainContainer = findViewById(R.id.mainContainer);
        mainContainer.setOnClickListener(this);
        viewContainer = findViewById(R.id.viewContainer);
        viewContainer.setOnTouchListener(this);
        background = findViewById(R.id.backgroundLayer);
        textView = findViewById(R.id.textView);

    }

    private void setBasicMeasurementVariables() {

        screen_width = ViewUtils.getScreenSize(context)[0];
        screen_height = ViewUtils.getScreenSize(context)[1];

        min_width = targetView.getMeasuredWidth();
        min_height = targetView.getMeasuredHeight();

        max_width = viewContainer.getMeasuredWidth();
        max_height = viewContainer.getMeasuredHeight();

        min_center_left = ViewUtils.getLocationOnScreen(targetView)[0] + min_width / 2;
        min_center_top = ViewUtils.getLocationOnScreen(targetView)[1] + min_height / 2;

        max_center_left = ViewUtils.getLocationOnScreen(viewContainer)[0] + max_width / 2;
        max_center_top = ViewUtils.getLocationOnScreen(viewContainer)[1] + max_height / 2;

        min_scaleX = (float) min_width / (float) max_width;
        min_scaleY = (float) min_height / (float) max_height;

        min_translationX = (min_center_left - max_center_left);
        min_translationY = (min_center_top - max_center_top);

        max_translationX = viewContainer.getTranslationX();
        max_translationY = viewContainer.getTranslationY();

        max_background_alpha = 1f;
        min_background_alpha = 0f;

        max_textView_alpha = 1f;
        min_textView_alpha = 0f;
    }

    private void expand() {

        new FutureTask(new FutureTask.OnTaskRunListener() {
            @Override
            public void onTaskRun() {
                if (stateListener != null)
                    stateListener.onExpandStarted();
            }
        }).start(10);

        ObjectAnimator imageViewAnimator = ObjectAnimator.ofPropertyValuesHolder(viewContainer,
                PropertyValuesHolder.ofFloat(View.SCALE_X, min_scaleX, max_scaleX),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, min_scaleY, max_scaleY),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, min_translationX, max_translationX),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, min_translationY, max_translationY));
        imageViewAnimator.setInterpolator(new FastOutSlowInInterpolator());
        imageViewAnimator.setDuration(300);

        ObjectAnimator fadeInBackground = ObjectAnimator.ofFloat(
                background, "alpha", min_background_alpha, max_background_alpha);
        fadeInBackground.setDuration(300);

        ObjectAnimator fadeInTextView = ObjectAnimator.ofFloat(
                textView, "alpha", min_textView_alpha, max_textView_alpha);
        fadeInTextView.setDuration(300);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(imageViewAnimator, fadeInBackground, fadeInTextView);

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (stateListener != null)
                    stateListener.onExpandEnded();
            }
        });

        animatorSet.start();
    }

    private void minimize() {

        if (stateListener != null)
            stateListener.onMinimizeStarted();

        closeAnimationCompleted = false;

        ObjectAnimator imageViewAnimator = ObjectAnimator.ofPropertyValuesHolder(viewContainer,
                PropertyValuesHolder.ofFloat(View.SCALE_X, min_scaleX),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, min_scaleY),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, min_translationX),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, min_translationY));
        imageViewAnimator.setInterpolator(new FastOutSlowInInterpolator());
        imageViewAnimator.setDuration(200);

        ObjectAnimator fadeOutBackground = ObjectAnimator.ofFloat(
                background, View.ALPHA, min_background_alpha);
        fadeOutBackground.setDuration(200);

        ObjectAnimator fadeOutTextView = ObjectAnimator.ofFloat(
                textView, View.ALPHA, min_textView_alpha);
        fadeOutTextView.setDuration(200);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(imageViewAnimator, fadeOutBackground, fadeOutTextView);

        new FutureTask(new FutureTask.OnTaskRunListener() {
            @Override
            public void onTaskRun() {
                if (stateListener != null)
                    stateListener.onMinimizeEnded();
            }
        }).start(imageViewAnimator.getDuration() - 10);

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                closeAnimationCompleted = true;
                Magnifier.this.dismiss();
            }
        });

        animatorSet.start();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();

                textView.animate()
                        .alpha(min_textView_alpha)
                        .setDuration(200)
                        .start();

                break;

            case MotionEvent.ACTION_MOVE:
                dismissGestureDetected = false;

                float offsetX = event.getRawX() + dX;
                float offsetY = event.getRawY() + dY;

                float translationOffset = (float) Math.hypot(view.getTranslationX(), view.getTranslationY());
                float maxTranslationOffset = Math.min(max_center_left, max_center_top);
                float alphaPercent = translationOffset / maxTranslationOffset;

                float backgroundAlphaValue = (1f - alphaPercent) * max_background_alpha;

                view.animate()
                        .x(offsetX)
                        .y(offsetY)
                        .setDuration(0)
                        .start();

                background.animate()
                        .alpha(backgroundAlphaValue)
                        .setDuration(0)
                        .start();


                if (translationOffset > maxTranslationOffset) {
                    dismissGestureDetected = true;
                }

                break;

            case MotionEvent.ACTION_UP:
                if (dismissGestureDetected) {
                    dismiss();
                } else {
                    view.animate()
                            .translationX(max_translationX)
                            .translationY(max_translationY)
                            .setDuration(200)
                            .setInterpolator(new OvershootInterpolator())
                            .start();

                    background.animate()
                            .alpha(max_background_alpha)
                            .setDuration(200)
                            .start();

                    textView.animate()
                            .alpha(max_textView_alpha)
                            .setDuration(200)
                            .start();
                }
                break;

            default:
                return false;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mainContainer.getId()) {
            dismiss();
        }
    }

    @Override
    public void dismiss() {
        if (closeAnimationCompleted || instantDismissRequested) {
            targetView.setDrawingCacheEnabled(false);
            super.dismiss();
        } else {
            minimize();
        }
    }

    public void instantDismiss() {
        instantDismissRequested = true;
        dismiss();
    }

    public void setStateListener(stateListener stateListener) {
        this.stateListener = stateListener;
    }

    public interface stateListener {
        void onExpandStarted();

        void onExpandEnded();

        void onMinimizeStarted();

        void onMinimizeEnded();
    }
}
