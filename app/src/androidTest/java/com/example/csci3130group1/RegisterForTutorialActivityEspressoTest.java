package com.example.csci3130group1;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
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
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;

// RegisterForTutorialActivityEspressoTest class for Espresso tests for RegisterForTutorialActivity
@RunWith(AndroidJUnit4.class)
public class RegisterForTutorialActivityEspressoTest {

//    Attributes
    private String tutorialId;

//    setUp() method, mocks data for a sample free tutorial
    @Before
    public void setUp() {
        DatabaseReference tutorialsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        tutorialId = tutorialsRef.push().getKey();
        tutorialsRef.child(tutorialId).child("title").setValue("Espresso Test Tutorial");
        tutorialsRef.child(tutorialId).child("fee").setValue("0");
        tutorialsRef.child(tutorialId).child("city").setValue("Sample City");
        tutorialsRef.child(tutorialId).child("province").setValue("Sample Province");
        tutorialsRef.child(tutorialId).child("date").setValue("2025-12-31");
        tutorialsRef.child(tutorialId).child("time").setValue("10:00 AM");
        tutorialsRef.child(tutorialId).child("duration").setValue("45");
        tutorialsRef.child(tutorialId).child("description").setValue("Testing registration flow.");
    }

//    testNoTutorialInfoShowsErrorAndFinishes() method as a validation test
    @Test
    public void testNoTutorialInfoShowsErrorAndFinishes() {

//        Launches scenario with no extras
        ActivityScenario<RegisterForTutorialActivity> scenario =
                ActivityScenario.launch(RegisterForTutorialActivity.class);

//        Since there are missing extras, activity should terminate
        scenario.moveToState(Lifecycle.State.DESTROYED);
        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }

//    testUIElementsAndCancelButtonForFreeTutorial() method as a general test
    @Test
    public void testUIElementsAndCancelButtonForFreeTutorial() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), RegisterForTutorialActivity.class);
        intent.putExtra("tutorialId", tutorialId);
        intent.putExtra("tutorialTitle", "Espresso Test Tutorial");
        intent.putExtra("tutorialFee", "0");
        ActivityScenario<RegisterForTutorialActivity> scenario = ActivityScenario.launch(intent);

//        Added delay
        Thread.sleep(5000);

        onView(withId(R.id.tutorial_summary_text))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("Espresso Test Tutorial"))));

        onView(withId(R.id.pay_with_paypal_button))
                .check(matches(isDisplayed()));

        onView(withId(R.id.cancel_button))
                .perform(click());

        scenario.close();
    }

}
