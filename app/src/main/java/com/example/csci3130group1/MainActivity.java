package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;

public class MainActivity extends AppCompatActivity {

    private Button loginButton;
    private ImageView appIcon;
    private TextView registerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Reference ImageView
        appIcon = findViewById(R.id.app_icon);
        appIcon.setImageResource(R.drawable.app_icon2); // Set image programmatically
        View root = findViewById(R.id.main);

        // Center the logo vertically without affecting other views
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootHeight = root.getHeight();
                int iconHeight = appIcon.getHeight();
                int iconTop = appIcon.getTop();
                float targetTop = (rootHeight - iconHeight) / 2f;
                float delta = targetTop - iconTop;

                // Nudge the logo slightly upward (in dp)
                float offsetUpDp = 32f;
                float density = getResources().getDisplayMetrics().density;
                float offsetPx = offsetUpDp * density;

                appIcon.setTranslationY(delta - offsetPx);

                // Remove listener after first run
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        // Reference Button
        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Reference and set up Create Account text
        registerText = findViewById(R.id.register_text);
        registerText.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
