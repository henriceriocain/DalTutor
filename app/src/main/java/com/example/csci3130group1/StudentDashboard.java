package com.example.csci3130group1;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.csci3130group1.databinding.ActivityStudentDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentDashboard extends AppCompatActivity {

    private ActivityStudentDashboardBinding binding;
    private android.widget.TextView notifBadgeCount;
    private android.widget.ImageButton bell;
    private boolean onNotificationsScreen = false;
    private com.google.firebase.database.Query bizUnreadQuery;
    private com.google.firebase.database.Query comUnreadQuery;
    private com.google.firebase.database.ValueEventListener bizUnreadListener;
    private com.google.firebase.database.ValueEventListener comUnreadListener;
    private long unreadBizCount = 0L;
    private long unreadComCount = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStudentDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_profile, R.id.navigation_search_for_tutorials, R.id.navigation_community)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_student_dashboard);
        NavigationUI.setupWithNavController(binding.navView, navController);
        navView.getMenu().removeItem(R.id.navigation_tutorial_management);

        // Override default behavior to always navigate to tab destinations
        setupCustomTabNavigation(navView, navController);

        // FAB removed; creation entry lives in Profile's Tutor Tools

        // Notifications bell setup
        notifBadgeCount = findViewById(R.id.btn_notifications_badge_count);
        bell = findViewById(R.id.btn_notifications);
        if (bell != null) {
            bell.setElevation(8f);
            bell.bringToFront();
            bell.setOnClickListener(v -> navController.navigate(R.id.navigation_notifications));
        }
        if (notifBadgeCount != null) {
            notifBadgeCount.setElevation(9f);
            notifBadgeCount.bringToFront();
        }

        // Start listening for badge updates
        attachNotificationBadgeListeners();

        // Hide bell (and badge) while on notifications screen
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_notifications) {
                onNotificationsScreen = true;
                if (bell != null) bell.setVisibility(android.view.View.GONE);
                if (notifBadgeCount != null) notifBadgeCount.setVisibility(android.view.View.GONE);
            } else {
                onNotificationsScreen = false;
                if (bell != null) bell.setVisibility(android.view.View.VISIBLE);
                updateBadge(unreadBizCount + unreadComCount);
            }
        });
    }

    private void setupCustomTabNavigation(BottomNavigationView navView, NavController navController) {
        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // Always navigate to the main destination, even if it's currently selected
            // This ensures users can return to the main tab page from sub-pages like notifications
            try {
                if (itemId == R.id.navigation_profile) {
                    navController.navigate(R.id.navigation_profile);
                    return true;
                } else if (itemId == R.id.navigation_search_for_tutorials) {
                    navController.navigate(R.id.navigation_search_for_tutorials);
                    return true;
                } else if (itemId == R.id.navigation_community) {
                    navController.navigate(R.id.navigation_community);
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // Handle case where navigation fails (destination not found, etc.)
                // Fall back to letting NavigationUI handle it
                return NavigationUI.onNavDestinationSelected(item, navController);
            }
            
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure listeners active
        attachNotificationBadgeListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachNotificationBadgeListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachNotificationBadgeListeners();
    }

    private void refreshNotificationBadge() {
        // Legacy one-shot refresh (kept as utility)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { updateBadge(0); return; }
        DatabaseReference bizRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("notifications");
        DatabaseReference comRef = FirebaseDatabase.getInstance().getReference("community_notifications").child(user.getUid());
        bizRef.orderByChild("read").equalTo(false).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { unreadBizCount = snapshot.getChildrenCount(); updateBadge(unreadBizCount + unreadComCount); }
            @Override public void onCancelled(DatabaseError error) { updateBadge(unreadBizCount + unreadComCount); }
        });
        comRef.orderByChild("read").equalTo(false).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { unreadComCount = snapshot.getChildrenCount(); updateBadge(unreadBizCount + unreadComCount); }
            @Override public void onCancelled(DatabaseError error) { updateBadge(unreadBizCount + unreadComCount); }
        });
    }

    private void attachNotificationBadgeListeners() {
        if (notifBadgeCount == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { updateBadge(0); return; }

        // Detach any existing to avoid duplicates
        detachNotificationBadgeListeners();

        bizUnreadQuery = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("notifications")
                .orderByChild("read").equalTo(false);
        comUnreadQuery = FirebaseDatabase.getInstance()
                .getReference("community_notifications")
                .child(user.getUid())
                .orderByChild("read").equalTo(false);

        bizUnreadListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                unreadBizCount = snapshot.getChildrenCount();
                updateBadge(unreadBizCount + unreadComCount);
            }
            @Override public void onCancelled(DatabaseError error) { /* ignore */ }
        };
        comUnreadListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                unreadComCount = snapshot.getChildrenCount();
                updateBadge(unreadBizCount + unreadComCount);
            }
            @Override public void onCancelled(DatabaseError error) { /* ignore */ }
        };

        if (bizUnreadQuery != null) bizUnreadQuery.addValueEventListener(bizUnreadListener);
        if (comUnreadQuery != null) comUnreadQuery.addValueEventListener(comUnreadListener);
    }

    private void detachNotificationBadgeListeners() {
        if (bizUnreadQuery != null && bizUnreadListener != null) {
            bizUnreadQuery.removeEventListener(bizUnreadListener);
        }
        if (comUnreadQuery != null && comUnreadListener != null) {
            comUnreadQuery.removeEventListener(comUnreadListener);
        }
    }

    private void updateBadge(long count) {
        if (notifBadgeCount == null) return;
        if (onNotificationsScreen) {
            notifBadgeCount.setVisibility(android.view.View.GONE);
            return;
        }
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
}
