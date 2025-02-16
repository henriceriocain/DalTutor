package com.example.csci3130group1;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LogoutTest {

    @Rule
    public ActivityTestRule<HomeActivity> activityRule =
            new ActivityTestRule<>(HomeActivity.class, false, false);

    @Test
    public void testLogoutFunctionality() {
        // Start HomeActivity to simulate a logged-in user
        activityRule.launchActivity(new Intent());

        // Check if logout button is displayed
        onView(withId(R.id.logout_button)).check(matches(isDisplayed()));

        // Click the logout button
        onView(withId(R.id.logout_button)).perform(click());

        // Ensure that the LoginActivity is displayed after logout
        onView(withId(R.id.button2)).check(matches(isDisplayed()));
    }

    @Test
    public void testLogoutButtonIsDisplayed() {
        // Start HomeActivity
        activityRule.launchActivity(new Intent());

        // Verify that the logout button is visible
        onView(withId(R.id.logout_button)).check(matches(isDisplayed()));
    }

    @Test
    public void testLogoutNavigatesToLoginActivity() {
        // Start HomeActivity
        activityRule.launchActivity(new Intent());

        // Click the logout button
        onView(withId(R.id.logout_button)).perform(click());

        // Verify that LoginActivity is displayed by checking login button presence
        onView(withId(R.id.button2)).check(matches(isDisplayed()));
    }


}
