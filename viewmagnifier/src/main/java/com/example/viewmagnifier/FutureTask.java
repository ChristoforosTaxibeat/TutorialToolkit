package com.example.viewmagnifier;

import android.os.Handler;

/**
 * Created by christoforoskolovos on 06/10/2017.
 */

public class FutureTask {
    private Handler handler;
    private Runnable runnable;

    public FutureTask(final OnTaskRunListener listener) {
        runnable = new Runnable() {
            @Override
            public void run() {
                listener.onTaskRun();
            }
        };
        handler = new Handler();

    }

    public void start(long timeInFuture) {
        handler.postDelayed(runnable, timeInFuture);
    }

    public void cancel() {
        handler.removeCallbacks(runnable);
    }

    public interface OnTaskRunListener {
        void onTaskRun();
    }
}