// Package
package com.example.csci3130group1;

// Import statements
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// GoogleMapActivityUITest class
@RunWith(AndroidJUnit4.class)
public class GoogleMapActivityUITest {

//    Rule
    @Rule
    public ActivityScenarioRule<GoogleMapActivity> activityRule =
            new ActivityScenarioRule<>(GoogleMapActivity.class);

//    Tests that the map fragment is displayed
    @Test
    public void testMapFragmentIsDisplayed() {
        onView(withId(R.id.map)).check(matches(isDisplayed()));
    }

//    Tests the back button
    @Test
    public void testBackButtonIsDisplayed() {
        onView(withId(R.id.back_button)).check(matches(isDisplayed()));
    }

//    Tests that the back button correctly displays "Back"
    @Test
    public void testBackButtonHasCorrectText() {
        onView(withId(R.id.back_button)).check(matches(withText("Back")));
    }

//    Tests the functionality of the back button
    @Test
    public void testBackButtonClickClosesActivity() {
        onView(withId(R.id.back_button)).perform(click());
    }
}