package com.example.csci3130group1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

// RegistrationConfirmationActivityUITest UI automator tests for RegistrationConfirmationActivity
// Used Henri's credentials to sign in as user sign-in is crucial for registration confirmation
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RegistrationConfirmationActivityUITest {

//    Attributes
    private static final String PACKAGE_NAME = "com.example.csci3130group1";
    private static final long LAUNCH_TIMEOUT = 5000L;
    private UiDevice device;
    private String tutorialId;
    private final Context context = ApplicationProvider.getApplicationContext();

//    setUp() method
    @Before
    public void setUp() throws Exception {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

//        Navigates to home screen
        device.pressHome();
        device.wait(Until.hasObject(By.pkg(device.getLauncherPackageName()).depth(0)), LAUNCH_TIMEOUT);

//        Signs users out in case
        FirebaseAuth.getInstance().signOut();

//        Mock tutorial data
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        tutorialId = ref.push().getKey();
        ref.child(tutorialId).child("title").setValue("UIAutomator Test Tutorial");
        ref.child(tutorialId).child("city").setValue("Sample City");
        ref.child(tutorialId).child("province").setValue("Sample Province");
        ref.child(tutorialId).child("duration").setValue("30");
        ref.child(tutorialId).child("fee").setValue("0");
        ref.child(tutorialId).child("name").setValue("Test Tutor");
    }

//    testFinishesWhenUserIsNull() method to test when user is null
    @Test
    public void testFinishesWhenUserIsNull() throws Exception {

//        Launches the activity with extras
        Intent intent = new Intent(context, RegistrationConfirmationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("tutorialId", tutorialId);
        intent.putExtra("tutorialTitle", "UIAutomator Test Tutorial");
        intent.putExtra("tutorialFee", "0");
        intent.putExtra("paymentId", "FAKE_PAYMENT_ID");
        intent.putExtra("paymentTime", "2025-12-31T15:00:00Z");
        context.startActivity(intent);

//        Delay
        Thread.sleep(1000);

//        Checks that the activity closes
        UiObject confirmationText = device.findObject(
                new UiSelector().resourceIdMatches(PACKAGE_NAME + ":id/confirmation_text"));
        boolean exists = confirmationText.waitForExists(1500);
        assertFalse("Activity stayed alive when it shouldn't", exists);
    }

//    signInUser() helper method to sign me (henri) in
    private boolean signInUser(String email, String password) throws InterruptedException {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();

        final boolean[] result = { false };
        CountDownLatch latch = new CountDownLatch(1);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        result[0] = true;
                    }
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);
        return result[0];
    }
}
