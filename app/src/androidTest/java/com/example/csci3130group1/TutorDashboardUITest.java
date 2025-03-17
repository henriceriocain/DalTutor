package com.example.csci3130group1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TutorDashboardUITest {
    private static final int TIMEOUT = 5000; // Adjusted timeout for stability
    private UiDevice device;

    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Context context = ApplicationProvider.getApplicationContext();
        Intent launchIntent = new Intent(context, TutorDashboard.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);
        device.wait(Until.hasObject(By.pkg("com.example.csci3130group1").depth(0)), TIMEOUT);
    }

    // ✅ Test if the app launches and shows the welcome text
    @Test
    public void testAppLaunchesSuccessfully() {
        UiObject welcomeText = device.findObject(new UiSelector().resourceId("com.example.csci3130group1:id/welcome_text"));

        assertTrue("TutorDashboard did not load properly!", welcomeText.waitForExists(TIMEOUT));
    }

    // ✅ Ensure that search and preferences menu items are removed
    @Test
    public void testSearchAndPreferencesMenuItemsRemoved() {
        UiObject searchItem = device.findObject(new UiSelector().text("Search for Tutorials"));
        UiObject preferencesItem = device.findObject(new UiSelector().text("Manage Preferences"));

        assertFalse("Search for Tutorials should not exist!", searchItem.exists());
        assertFalse("Manage Preferences should not exist!", preferencesItem.exists());
    }

    // ✅ Verify that bottom navigation bar is displayed correctly
    @Test
    public void testBottomNavigationBarDisplayed() {
        UiObject bottomNav = device.findObject(new UiSelector().resourceId("com.example.csci3130group1:id/nav_view"));
        assertTrue("Bottom navigation bar is not displayed!", bottomNav.waitForExists(TIMEOUT));
    }

    // ✅ Ensure the "Profile" tab is visible
    @Test
    public void testProfileTabExists() {
        UiObject profileTab = device.findObject(new UiSelector().text("Profile"));
        assertTrue("Profile tab is missing!", profileTab.waitForExists(TIMEOUT));
    }

}
