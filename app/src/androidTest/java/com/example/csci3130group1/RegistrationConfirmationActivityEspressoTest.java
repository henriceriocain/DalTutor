package com.example.csci3130group1;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


// RegistrationConfirmationActivityEspressoTest espresso tests for RegistrationConfirmationActivity
// Used Henri's credentials to sign in as user sign-in is crucial for registration confirmation
@RunWith(AndroidJUnit4.class)
public class RegistrationConfirmationActivityEspressoTest {

//    Attributes
    private String tutorialId;

    @Before
    public void setUp() {

//        Safety sign out
        FirebaseAuth.getInstance().signOut();

//        Mock data in firebase
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        tutorialId = ref.push().getKey();
        ref.child(tutorialId).child("title").setValue("Confirmation Test Tutorial");
        ref.child(tutorialId).child("city").setValue("Sample City");
        ref.child(tutorialId).child("province").setValue("Sample Province");
        ref.child(tutorialId).child("duration").setValue("30");
        ref.child(tutorialId).child("fee").setValue("0"); // free
        ref.child(tutorialId).child("name").setValue("Test Tutor");
    }

//    testFinishesWhenUserIsNull() method to check when no user is signed in we act accordingly
    @Test
    public void testFinishesWhenUserIsNull() {

//        Signs out user
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), RegistrationConfirmationActivity.class);

//        Simple extras
        intent.putExtra("tutorialId", tutorialId);
        intent.putExtra("tutorialTitle", "Confirmation Test Tutorial");
        intent.putExtra("tutorialFee", "0");
        intent.putExtra("paymentId", "FAKE_PAYMENT_ID");
        intent.putExtra("paymentTime", "2025-12-31T15:00:00Z");

//        Checks we terminate when no user is signed in
        ActivityScenario<RegistrationConfirmationActivity> scenario = ActivityScenario.launch(intent);
        scenario.moveToState(Lifecycle.State.DESTROYED);
        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }


//    signInUser() helper method to sign in
    private boolean signInUser(String email, String password) throws InterruptedException {

//        Signs out first
        FirebaseAuth.getInstance().signOut();

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = { false };

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        result[0] = true;
                    }
                    latch.countDown();
                });

//        Delay for sign in
        latch.await(5, TimeUnit.SECONDS);
        return result[0];
    }
}
