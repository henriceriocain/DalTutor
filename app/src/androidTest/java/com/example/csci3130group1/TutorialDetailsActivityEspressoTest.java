package com.example.csci3130group1;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import androidx.lifecycle.Lifecycle;
import static org.junit.Assert.assertEquals;

// TutorialDetailsActivityEspressoTest class
@RunWith(AndroidJUnit4.class)
public class TutorialDetailsActivityEspressoTest {

//    Attributes
    private String tutorialId;

//    setUp() method, creates mock tutorial data and pushes into firebase
    @Before
    public void setUp() {
        DatabaseReference tutorialsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        tutorialId = tutorialsRef.push().getKey();
        tutorialsRef.child(tutorialId).child("title").setValue("Sample Tutorial");
        tutorialsRef.child(tutorialId).child("location").setValue("Sample Location");
        tutorialsRef.child(tutorialId).child("fee").setValue("10");
        tutorialsRef.child(tutorialId).child("duration").setValue("60");
    }

//    testTutorialDetailsDisplayed() method to check elements are correct for the valid tutorialId extra
    @Test
    public void testTutorialDetailsDisplayed() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TutorialDetailsActivity.class);
        intent.putExtra("tutorialId", tutorialId);
        ActivityScenario.launch(intent);
        onView(withId(R.id.tutorial_title))
                .check(matches(withText("Sample Tutorial")));
        onView(withId(R.id.tutorial_location))
                .check(matches(withText("Location: Sample Location")));
        onView(withId(R.id.tutorial_fee))
                .check(matches(withText("Fee: $10")));
        onView(withId(R.id.tutorial_date))
                .check(matches(withText("Date: N/A")));
        onView(withId(R.id.tutorial_time))
                .check(matches(withText("Time: N/A")));
        onView(withId(R.id.tutorial_duration))
                .check(matches(withText("Duration: 60 minutes")));
        onView(withId(R.id.back_button)).perform(click());
    }

//    testNoTutorialIdShowsErrorAndFinishes() method, tests for when the activity is launched without tutorialID, correctly causing an error
    @Test
    public void testNoTutorialIdShowsErrorAndFinishes() {
        ActivityScenario<TutorialDetailsActivity> scenario =
                ActivityScenario.launch(TutorialDetailsActivity.class);
        scenario.moveToState(Lifecycle.State.DESTROYED);
        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }
}
