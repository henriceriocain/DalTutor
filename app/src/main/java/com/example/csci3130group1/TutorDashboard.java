package com.example.csci3130group1;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.csci3130group1.databinding.ActivityTutorDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TutorDashboard extends AppCompatActivity {
    private ActivityTutorDashboardBinding binding;
    private TextView welcomeText;
    private TextView notifBadgeCount;
    private android.widget.ImageButton bell;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTutorDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_profile, R.id.navigation_search_for_tutorials, R.id.navigation_community)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_tutor_dashboard);
        NavigationUI.setupWithNavController(binding.navView, navController);
        // Remove non-tab items for tutors
        navView.getMenu().removeItem(R.id.navigation_tutorial_management);
        welcomeText = findViewById(R.id.welcome_text);
        notifBadgeCount = findViewById(R.id.btn_notifications_badge_count);
// NEW: Get username, role, and password from intent
        String username = getIntent().getStringExtra("username");
        String role = getIntent().getStringExtra("role");
        String password = getIntent().getStringExtra("password");

        // NEW: Display personalized welcome and toast
        if (username != null && role != null) {
            welcomeText.setText("Hello and welcome " + username + "! You are logged in as a " + role);
            Toast.makeText(this, "Logged in as " + username + " (" + role + ")", Toast.LENGTH_LONG).show();
        }

        // Floating Action Button to create a tutorial (navigates to Tutorial Management)
        FloatingActionButton fab = findViewById(R.id.fab_create_tutorial);
        if (fab != null) {
            fab.setOnClickListener(v -> navController.navigate(R.id.navigation_tutorial_management));
        }

        // Top-right bell icon (borderless ImageButton)
        bell = findViewById(R.id.btn_notifications);
        if (bell != null) {
            bell.setOnClickListener(v -> navController.navigate(R.id.navigation_notifications));
        }

        refreshNotificationBadge();

        // Hide bell (and badge) while on notifications screen
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_notifications) {
                if (bell != null) bell.setVisibility(android.view.View.GONE);
                if (notifBadgeCount != null) notifBadgeCount.setVisibility(android.view.View.GONE);
            } else {
                if (bell != null) bell.setVisibility(android.view.View.VISIBLE);
                // Only show badge if there are unread business notifications
                refreshNotificationBadge();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotificationBadge();
    }

    private void refreshNotificationBadge() {
        if (notifBadgeCount == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            notifBadgeCount.setVisibility(android.view.View.GONE);
            return;
        }

        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("notifications");

        notifRef.orderByChild("read").equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        if (count > 0) {
                            notifBadgeCount.setVisibility(android.view.View.VISIBLE);
                            if (count > 99) {
                                notifBadgeCount.setText("99+");
                            } else {
                                notifBadgeCount.setText(String.valueOf(count));
                            }
                        } else {
                            notifBadgeCount.setVisibility(android.view.View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        notifBadgeCount.setVisibility(android.view.View.GONE);
                    }
                });
    }
}
