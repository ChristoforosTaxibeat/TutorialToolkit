package com.example.viewmagnifier;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
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
import android.view.ViewGroup;
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
    float scale, min_translationX, min_translationY,
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
        this.scale = scale;

        initViews();

        setView();

        viewContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                viewContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                setBasicMeasurementVariables();
                setText();
                expand();
                return true;
            }
        });
    }

    private void setView() {
        targetView.setDrawingCacheEnabled(true);
        Bitmap bitmap = targetView.getDrawingCache();
        viewContainer.setImageBitmap(bitmap);
    }

    private void setText() {
        textView.setText(text);
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

        max_width = (int) (min_width * scale);
        max_height = (int) (min_height * scale);

        min_center_left = ViewUtils.getLocationOnScreen(targetView)[0] + max_width / 2;
        min_center_top = ViewUtils.getLocationOnScreen(targetView)[1] + max_height / 2;

        max_center_left = ViewUtils.getLocationOnScreen(viewContainer)[0] + max_width / 2;
        max_center_top = ViewUtils.getLocationOnScreen(viewContainer)[1] + max_height / 2;


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

        ValueAnimator widthAnimator = ValueAnimator.ofInt(min_width, max_width);
        widthAnimator.setTarget(viewContainer);
        widthAnimator.setInterpolator(new FastOutSlowInInterpolator());
        widthAnimator.setDuration(300);
        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams layoutparams = viewContainer.getLayoutParams();
                layoutparams.width = (int) valueAnimator.getAnimatedValue();
                viewContainer.setLayoutParams(layoutparams );
            }
        });

        ValueAnimator heightAnimator = ValueAnimator.ofInt(min_height, max_height);
        heightAnimator.setTarget(viewContainer);
        heightAnimator.setInterpolator(new FastOutSlowInInterpolator());
        heightAnimator.setDuration(300);
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams layoutparams = viewContainer.getLayoutParams();
                layoutparams.height = (int) valueAnimator.getAnimatedValue();
                viewContainer.setLayoutParams(layoutparams );
            }
        });

        ObjectAnimator translationAnimator = ObjectAnimator.ofPropertyValuesHolder(viewContainer,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, min_translationX, max_translationX),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, min_translationY, max_translationY));
        translationAnimator.setInterpolator(new FastOutSlowInInterpolator());
        translationAnimator.setDuration(300);

        ObjectAnimator fadeInBackground = ObjectAnimator.ofFloat(
                background, "alpha", min_background_alpha, max_background_alpha);
        fadeInBackground.setDuration(300);

        ObjectAnimator fadeInTextView = ObjectAnimator.ofFloat(
                textView, "alpha", min_textView_alpha, max_textView_alpha);
        fadeInTextView.setDuration(300);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(widthAnimator, heightAnimator, translationAnimator, fadeInBackground, fadeInTextView);

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

        ValueAnimator widthAnimator = ValueAnimator.ofInt(max_width, min_width);
        widthAnimator.setTarget(viewContainer);
        widthAnimator.setInterpolator(new FastOutSlowInInterpolator());
        widthAnimator.setDuration(300);
        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams layoutparams = viewContainer.getLayoutParams();
                layoutparams.width = (int) valueAnimator.getAnimatedValue();
                viewContainer.setLayoutParams(layoutparams );
            }
        });

        ValueAnimator heightAnimator = ValueAnimator.ofInt(max_height, min_height);
        heightAnimator.setTarget(viewContainer);
        heightAnimator.setInterpolator(new FastOutSlowInInterpolator());
        heightAnimator.setDuration(300);
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams layoutparams = viewContainer.getLayoutParams();
                layoutparams.height = (int) valueAnimator.getAnimatedValue();
                viewContainer.setLayoutParams(layoutparams );
            }
        });

        ObjectAnimator translationAnimator = ObjectAnimator.ofPropertyValuesHolder(viewContainer,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, min_translationX),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, min_translationY));
        translationAnimator.setInterpolator(new FastOutSlowInInterpolator());
        translationAnimator.setDuration(200);

        ObjectAnimator fadeOutBackground = ObjectAnimator.ofFloat(
                background, View.ALPHA, min_background_alpha);
        fadeOutBackground.setDuration(200);

        ObjectAnimator fadeOutTextView = ObjectAnimator.ofFloat(
                textView, View.ALPHA, min_textView_alpha);
        fadeOutTextView.setDuration(200);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(widthAnimator, heightAnimator, translationAnimator, fadeOutBackground, fadeOutTextView);

        new FutureTask(new FutureTask.OnTaskRunListener() {
            @Override
            public void onTaskRun() {
                if (stateListener != null)
                    stateListener.onMinimizeEnded();
            }
        }).start(translationAnimator.getDuration() - 10);

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
