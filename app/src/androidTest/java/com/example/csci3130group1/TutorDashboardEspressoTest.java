package com.example.csci3130group1;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import android.os.Looper;
import android.os.Handler;


import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TutorDashboardEspressoTest {

    @Rule
    public ActivityScenarioRule<TutorDashboard> activityScenarioRule =
            new ActivityScenarioRule<>(TutorDashboard.class);


    // ✅ Verify that search and preferences menu items are removed
    @Test
    public void testSearchAndPreferencesMenuItemsRemoved() {
        ActivityScenario<TutorDashboard> scenario = activityScenarioRule.getScenario();
        scenario.onActivity(activity -> {
            BottomNavigationView navView = activity.findViewById(R.id.nav_view);
            assert (navView.getMenu().findItem(R.id.navigation_search_for_tutorials) == null);
            assert (navView.getMenu().findItem(R.id.navigation_manage_preferences) == null);
        });
    }

    // ✅ Validate toast message
    @Test
    public void testToastMessageDisplaysCorrectly() {
        ActivityScenario<TutorDashboard> scenario = activityScenarioRule.getScenario();
        scenario.onActivity(activity -> {
            String expectedToast = "TestUser-password123-Tutor";

            // Ensure the Toast is displayed before checking
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                onView(withText(expectedToast))
                        .inRoot(withDecorView(not(is(activity.getWindow().getDecorView()))))
                        .check(matches(isDisplayed()));
            }, 2000); // Delay for 2 seconds
        });
    }

}


