package com.example.csci3130group1.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.csci3130group1.R;

public class FeeStyleUtils {

    public static boolean isFree(String fee) {
        if (fee == null) return true;
        String clean = fee.replaceAll("[^\\d.]", "");
        return clean.isEmpty() || clean.equals("0") || clean.equals("0.0") || clean.equals("0.00");
    }

    public static void apply(TextView view, String fee, Context ctx) {
        if (view == null || ctx == null) return;
        if (isFree(fee)) {
            view.setText("Free");
            view.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary)); // subtle gray
            view.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        } else {
            view.setText("$" + fee);
            view.setTextColor(ContextCompat.getColor(ctx, R.color.brown_primary));
            view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }
    }
}

