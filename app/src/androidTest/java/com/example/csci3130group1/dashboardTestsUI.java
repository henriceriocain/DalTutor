package com.example.csci3130group1;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
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

public class dashboardTestsUI {
    private static final int LAUNCH_TIMEOUT = 5000;
    final String launcherPackage = "com.example.csci3130group1";
    private UiDevice device;
    @Before
    public void setup() {
        device = UiDevice.getInstance(getInstrumentation());
        Context context = ApplicationProvider.getApplicationContext();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(launcherPackage);
        assert launchIntent != null;
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(launchIntent);
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);
    }
    @Test
    public void checkIfInTutorProfile() throws UiObjectNotFoundException {
        UiObject enterLogin = device.findObject(new UiSelector().text("Login"));
        enterLogin.clickAndWaitForNewWindow();
        UiObject emailBox = device.findObject(new UiSelector().text("Email"));
        emailBox.setText("netherfire51@gmail.com");
        UiObject passwordBox = device.findObject(new UiSelector().text("Password"));
        passwordBox.setText("Gavin26672!");
        UiObject roleSpinner = device.findObject(new UiSelector().text("Select your role"));
        roleSpinner.click();
        UiObject tutorRole = device.findObject(new UiSelector().resourceId("android:id/text1").text("Tutor"));
        tutorRole.click();
        UiObject login = device.findObject(new UiSelector().text("Login"));
        login.clickAndWaitForNewWindow();
        UiObject viewRecommendations = device.findObject(new UiSelector().textContains("View Recommendations"));
        assertTrue(viewRecommendations.exists());
        UiObject tutorialManagement = device.findObject(new UiSelector().textContains("Tutorial Management"));
        assertTrue(tutorialManagement.exists());
    }
    @Test
    public void checkIfInStudentProfile() throws UiObjectNotFoundException {
        UiObject enterLogin = device.findObject(new UiSelector().text("Login"));
        enterLogin.clickAndWaitForNewWindow();
        UiObject emailBox = device.findObject(new UiSelector().text("Email"));
        emailBox.setText("gv749789@dal.ca");
        UiObject passwordBox = device.findObject(new UiSelector().text("Password"));
        passwordBox.setText("Gavin26672!");
        UiObject roleSpinner = device.findObject(new UiSelector().text("Select your role"));
        roleSpinner.click();
        UiObject tutorRole = device.findObject(new UiSelector().resourceId("android:id/text1").text("Student"));
        tutorRole.click();
        UiObject login = device.findObject(new UiSelector().text("Login"));
        login.clickAndWaitForNewWindow();
        UiObject searchTutorials = device.findObject(new UiSelector().textContains("Search for Tutorials"));
        assertTrue(searchTutorials.exists());
        UiObject managePreferences = device.findObject(new UiSelector().textContains("Manage Preferences"));
        assertTrue(managePreferences.exists());
    }
}
