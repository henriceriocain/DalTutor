package com.example.csci3130group1;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class US11_automator {

    private static final long LAUNCH_TIMEOUT = 5000; // ms
    private UiDevice device;

    @Before
    public void setUp() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Launch the app
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            // Clear out any previous instances
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }

        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(context.getPackageName()).depth(0)), LAUNCH_TIMEOUT);
    }

    @Test
    public void testManagePreferences() throws UiObjectNotFoundException {
        // Wait for the main UI to appear
        device.wait(Until.findObject(By.pkg("com.example.csci3130group1").depth(0)), LAUNCH_TIMEOUT);

        // Try to click the Login button or card first (if it exists)
        UiObject enterLogin = device.findObject(new UiSelector().textContains("Login"));
        if (enterLogin.exists() && enterLogin.isEnabled()) {
            enterLogin.clickAndWaitForNewWindow();
        }

        // Fill out login form
        UiObject emailBox = device.findObject(new UiSelector().textContains("Email"));
        emailBox.setText("gv749789@dal.ca");

        UiObject passwordBox = device.findObject(new UiSelector().textContains("Password"));
        passwordBox.setText("Gavin26672!");

        UiObject roleSpinner = device.findObject(new UiSelector().textContains("Select your role"));
        roleSpinner.click();

        UiObject studentRole = device.findObject(new UiSelector().text("Student"));
        studentRole.click();

        UiObject loginButton = device.findObject(new UiSelector().textContains("Login"));
        loginButton.clickAndWaitForNewWindow();

        // Wait until logged in (you can wait for a known dashboard element)
        device.wait(Until.findObject(By.textContains("Manage Preferences")), LAUNCH_TIMEOUT);

        // Tap "Manage Preferences"
        UiObject managePref = device.findObject(new UiSelector().textContains("Manage Preferences"));
        managePref.click();

        // Choose a topic like "Computer Science"
        UiObject csTopic = device.findObject(new UiSelector().textContains("CS"));
        csTopic.click();

        // Select tutor
        UiObject tutorSpinner = device.findObject(new UiSelector().textContains("Select a Tutor"));
        tutorSpinner.click();

        UiObject tutorFav = device.findObject(new UiSelector().text("Gavin Rainnie"));
        tutorFav.click();

        // Save preferences
        UiObject saveButton = device.findObject(new UiSelector().textContains("Save Preferences"));
        saveButton.click();
    }

}
