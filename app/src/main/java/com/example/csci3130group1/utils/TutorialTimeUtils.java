package com.example.csci3130group1.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TutorialTimeUtils {

    private TutorialTimeUtils() {}

    /**
     * Parses the end timestamp in milliseconds from date and end time fields.
     * Expected formats:
     * - date: "MMM dd, yyyy" (e.g., "Jan 31, 2025")
     * - endTime: "HH:mm" (24-hour, e.g., "14:30")
     * Returns null on parse failure or if inputs are null/empty.
     */
    public static Long parseEndMillis(String date, String endTime) {
        if (date == null || endTime == null) return null;
        String dateTime = date.trim() + " " + endTime.trim();
        SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        fmt.setLenient(false);
        fmt.setTimeZone(TimeZone.getTimeZone("America/Halifax"));
        try {
            Date d = fmt.parse(dateTime);
            return d != null ? d.getTime() : null;
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Parses the start timestamp in milliseconds from date and start time fields
     * using Halifax timezone. Returns null on parse failure.
     */
    public static Long parseStartMillis(String date, String startTime) {
        if (date == null || startTime == null) return null;
        String dateTime = date.trim() + " " + startTime.trim();
        SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        fmt.setLenient(false);
        fmt.setTimeZone(TimeZone.getTimeZone("America/Halifax"));
        try {
            Date d = fmt.parse(dateTime);
            return d != null ? d.getTime() : null;
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Returns true if the tutorial's end time is in the future (or now).
     * If parsing fails or required fields are missing, returns false.
     */
    public static boolean isUpcoming(String date, String endTime, long nowMillis) {
        Long endMillis = parseEndMillis(date, endTime);
        if (endMillis == null) return false;
        return endMillis >= nowMillis;
    }
}
