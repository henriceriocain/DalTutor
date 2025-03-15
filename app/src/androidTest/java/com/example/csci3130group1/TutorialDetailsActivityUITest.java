// Package
package com.example.csci3130group1;

// Import statements
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

// TutorialDetailsActivityUITest class
@RunWith(AndroidJUnit4.class)
public class TutorialDetailsActivityUITest {

//    Attributes
    private ActivityScenario<TutorialDetailsActivity> scenario;

//    setUp method
    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TutorialDetailsActivity.class);
        intent.putExtra("tutorialId", "mock-tutorial-id");
        scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> {
        });
    }

//    General test for elements
    @Test
    public void testTutorialDetailsElementsDisplayed() {
        onView(withId(R.id.tutorial_title)).check(matches(isDisplayed()));
        onView(withId(R.id.tutorial_location)).check(matches(isDisplayed()));
        onView(withId(R.id.tutorial_fee)).check(matches(isDisplayed()));
        onView(withId(R.id.tutorial_date)).check(matches(isDisplayed()));
        onView(withId(R.id.tutorial_time)).check(matches(isDisplayed()));
        onView(withId(R.id.tutorial_duration)).check(matches(isDisplayed()));
        onView(withId(R.id.back_button)).check(matches(isDisplayed()));
    }
}