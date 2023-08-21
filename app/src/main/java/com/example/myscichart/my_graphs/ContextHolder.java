package com.example.myscichart.my_graphs;

import android.content.Context;

public class ContextHolder {
    private static Context REF;

    public static void set(Context context) {
        REF = context;
    }

    public static Context get() {
        return REF;
    }
}
