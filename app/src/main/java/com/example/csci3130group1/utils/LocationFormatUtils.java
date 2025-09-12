package com.example.csci3130group1.utils;

import android.content.Context;
import android.text.TextUtils;

import java.util.List;

public class LocationFormatUtils {

    /**
     * Returns a user-friendly location string.
     * Priority:
     * 1) If placeId is a Dal place (dal:...), map to known pretty name from assets
     * 2) Else, use provided address if present
     * 3) Else, fall back to cleaned placeId (strip prefix, replace underscores)
     */
    public static String formatLocation(Context context, String address, String placeId) {
        // Try Dal place mapping first
        String prettyFromPlaceId = prettyDalPlaceName(context, placeId);
        if (!TextUtils.isEmpty(prettyFromPlaceId)) {
            return prettyFromPlaceId;
        }

        // Fallback to address
        if (!TextUtils.isEmpty(address)) {
            return address;
        }

        // Last resort: clean up raw placeId
        if (!TextUtils.isEmpty(placeId)) {
            String cleaned = placeId;
            if (cleaned.startsWith("dal:")) cleaned = cleaned.substring(4);
            cleaned = cleaned.replace('_', ' ');
            // Capitalize words lightly
            String[] parts = cleaned.split(" ");
            StringBuilder b = new StringBuilder();
            for (String p : parts) {
                if (p.isEmpty()) continue;
                b.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) b.append(p.substring(1).toLowerCase());
                b.append(' ');
            }
            return b.toString().trim();
        }

        return null;
    }

    private static String prettyDalPlaceName(Context context, String placeId) {
        if (TextUtils.isEmpty(placeId) || !placeId.startsWith("dal:")) return null;
        try {
            List<LocationSpinnerUtils.DalPlace> places = LocationSpinnerUtils.loadDalPlaces(context);
            for (LocationSpinnerUtils.DalPlace p : places) {
                String pid = p.getPlaceId();
                if (placeId.equalsIgnoreCase(pid)) {
                    // Return the human-friendly building name, e.g. "LSC Life Sciences Center"
                    return p.name;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}

