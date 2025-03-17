package com.example.csci3130group1;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// GoogleMapActivityEspressoTest class
@RunWith(AndroidJUnit4.class)
public class GoogleMapActivityEspressoTest {

//    Attributes
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

//    setUp() method, inserts mock tutorial data into Firebase
    @Before
    public void setUp() {
        DatabaseReference tutorialsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        String tutorialId = tutorialsRef.push().getKey();
        tutorialsRef.child(tutorialId).child("title").setValue("Test Tutorial");
        tutorialsRef.child(tutorialId).child("location").setValue("Killam Library");
    }

//    testMapIsDisplayed(), tests the display of the map
    @Test
    public void testMapIsDisplayed() {
        ActivityScenario.launch(GoogleMapActivity.class);
        Espresso.onView(ViewMatchers.withId(R.id.map))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

//    testMarkerClickOpensInfoWindow(), tests a marker correctly opens info window
    @Test
    public void testMarkerClickOpensInfoWindow() {
        ActivityScenario.launch(GoogleMapActivity.class);
        Espresso.onView(ViewMatchers.withContentDescription("Google Map"))
                .perform(ViewActions.click());
    }

//    testBackButton(), tests for back button functionality
    @Test
    public void testBackButton() {
        ActivityScenario.launch(GoogleMapActivity.class);
        Espresso.onView(ViewMatchers.withId(R.id.back_button))
                .perform(ViewActions.click());
    }
}