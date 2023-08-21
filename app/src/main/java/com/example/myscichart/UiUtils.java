package com.example.myscichart;

import android.app.Activity;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class UiUtils {

    public static int dpToPx(final Resources res, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                res.getDisplayMetrics());
    }

}
