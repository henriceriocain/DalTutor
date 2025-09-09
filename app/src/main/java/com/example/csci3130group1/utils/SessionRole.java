package com.example.csci3130group1.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.csci3130group1.SecureStorage;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Centralized helper for session role behavior.
 */
public final class SessionRole {
    private static final String KEY = "sessionRole";

    private SessionRole() {}

    public enum Role { STUDENT, TUTOR, UNKNOWN }

    public static void set(Context ctx, String roleString) {
        Role role = fromString(roleString);
        set(ctx, role);
    }

    public static void set(Context ctx, Role role) {
        try {
            SharedPreferences prefs = SecureStorage.getEncryptedSharedPreferences(ctx);
            String val = toString(role);
            if (val == null) {
                prefs.edit().remove(KEY).apply();
            } else {
                prefs.edit().putString(KEY, val).apply();
            }
        } catch (Exception ignored) {}
    }

    public static void clear(Context ctx) {
        try {
            SharedPreferences prefs = SecureStorage.getEncryptedSharedPreferences(ctx);
            prefs.edit().remove(KEY).apply();
        } catch (Exception ignored) {}
    }

    public static Role get(Context ctx) {
        try {
            SharedPreferences prefs = SecureStorage.getEncryptedSharedPreferences(ctx);
            String val = prefs.getString(KEY, null);
            return fromString(val);
        } catch (Exception e) {
            return Role.UNKNOWN;
        }
    }

    public static boolean isStudent(Context ctx) { return get(ctx) == Role.STUDENT; }
    public static boolean isTutor(Context ctx) { return get(ctx) == Role.TUTOR; }

    public interface RoleCallback { void onResolved(Role role); }

    /**
     * Resolve role using session preference first; if unknown and user provided, fallback to DB.
     */
    public static void resolveWithFallback(@NonNull Context ctx, FirebaseUser user, @NonNull RoleCallback cb) {
        Role r = get(ctx);
        if (r != Role.UNKNOWN) {
            cb.onResolved(r);
            return;
        }
        if (user == null) {
            cb.onResolved(Role.UNKNOWN);
            return;
        }
        FirebaseDatabase.getInstance()
                .getReference("users").child(user.getUid()).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String roleStr = snapshot.getValue(String.class);
                        Role role = fromString(roleStr);
                        cb.onResolved(role);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        cb.onResolved(Role.UNKNOWN);
                    }
                });
    }

    private static Role fromString(String s) {
        if (s == null) return Role.UNKNOWN;
        if ("Student".equalsIgnoreCase(s)) return Role.STUDENT;
        if ("Tutor".equalsIgnoreCase(s)) return Role.TUTOR;
        return Role.UNKNOWN;
    }

    private static String toString(Role r) {
        if (r == null) return null;
        switch (r) {
            case STUDENT: return "Student";
            case TUTOR: return "Tutor";
            default: return null;
        }
    }
}

