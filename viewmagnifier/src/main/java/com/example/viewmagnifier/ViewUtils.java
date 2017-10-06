package com.example.viewmagnifier;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;

/**
 * Created by christoforoskolovos on 06/10/2017.
 */

public class ViewUtils {

    public static int[] getLocationOnScreen(View view) {
        int[] location = new int[2];

        view.getLocationOnScreen(location);

        return location;
    }

    public static int[] getScreenSize(Context context) {
        int[] size = new int[2];

        size[0] = context.getResources().getDisplayMetrics().widthPixels;
        size[1] = context.getResources().getDisplayMetrics().heightPixels;

        return size;
    }

    public static int dpToPx(Resources res, float dp) {
        final float scale = res.getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static float pxToDp(Resources res, int px) {
        return px / res.getDisplayMetrics().density;
    }
}
