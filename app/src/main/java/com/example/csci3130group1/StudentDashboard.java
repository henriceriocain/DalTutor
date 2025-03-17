package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.csci3130group1.databinding.ActivityStudentDashboardBinding;

public class StudentDashboard extends AppCompatActivity {

    private ActivityStudentDashboardBinding binding;
    private TextView welcomeText;
    private Button mapButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStudentDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_profile, R.id.navigation_search_for_tutorials, R.id.navigation_manage_preferences)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_student_dashboard);
        NavigationUI.setupWithNavController(binding.navView, navController);
        navView.getMenu().removeItem(R.id.navigation_tutorial_management);
        navView.getMenu().removeItem(R.id.navigation_recommendations);
        welcomeText = findViewById(R.id.welcome_text);
        mapButton = findViewById(R.id.map_button);
        // NEW: Get username, role, and password from intent
        String username = getIntent().getStringExtra("username");
        String role = getIntent().getStringExtra("role");
        String password = getIntent().getStringExtra("password");

        // NEW: Display personalized welcome and toast
        if (username != null && role != null) {
            welcomeText.setText("Hello and welcome " + username + "! You are logged in as a " + role);
            Toast.makeText(this, username + "-" + password + "-" + role, Toast.LENGTH_LONG).show();
        }

//        Map button functionality
        mapButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboard.this, GoogleMapActivity.class);
            startActivity(intent);
        });
    }
}