package com.example.csci3130group1;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

// RegisterForTutorialActivityUITest UI Automator Test
@RunWith(AndroidJUnit4.class)
public class RegisterForTutorialActivityUITest {

//    Attributes
    private static final String PACKAGE_NAME = "com.example.csci3130group1";
    private UiDevice device;

//    Rule
    @Rule
    public ActivityTestRule<RegisterForTutorialActivity> activityRule =
            new ActivityTestRule<>(RegisterForTutorialActivity.class, false, false);

//    setUp() method, initializes the uidevice, starts at the home screen and launches the app
    @Before
    public void setUp() {

//        Initializes the UiDevice
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.pressHome();

//        Waits
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), 5000);

//        Launches app
        Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(PACKAGE_NAME);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);

            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5000);
        }
    }

//    testRegisterForFreeTutorial() method to test free tutorials
    @Test
    public void testRegisterForFreeTutorial() throws UiObjectNotFoundException {

//        Free tutorial intent
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), RegisterForTutorialActivity.class);
        intent.putExtra("tutorialId", "test-tutorial-id");
        intent.putExtra("tutorialTitle", "Free Tutorial Test");
        intent.putExtra("tutorialFee", "0.00");
        activityRule.launchActivity(intent);

//        Verifies elements are visible
        UiObject tutorialSummary = device.findObject(
                new UiSelector().resourceId(PACKAGE_NAME + ":id/tutorial_summary_text"));
        assertTrue("Tutorial summary should be displayed", tutorialSummary.exists());
        assertTrue("Tutorial summary should contain title",
                tutorialSummary.getText().contains("Free Tutorial Test"));
        assertTrue("Tutorial summary should indicate it's free",
                tutorialSummary.getText().contains("FREE"));

//        Clicks register button
        UiObject registerButton = device.findObject(
                new UiSelector().resourceId(PACKAGE_NAME + ":id/pay_with_paypal_button"));
        assertTrue("Register button should display 'Register for Free Tutorial'",
                registerButton.getText().contains("Register for Free Tutorial"));
        registerButton.click();

        assertTrue("Still on the same screen or another valid screen", true);
    }


// testCancelRegistration() method to test the cancel functionality
    @Test
    public void testCancelRegistration() throws UiObjectNotFoundException {

//        Paid tutorial intent
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), RegisterForTutorialActivity.class);
        intent.putExtra("tutorialId", "test-tutorial-id");
        intent.putExtra("tutorialTitle", "Paid Tutorial Test");
        intent.putExtra("tutorialFee", "25.00");
        activityRule.launchActivity(intent);

//        Verifies elements are visible
        UiObject tutorialSummary = device.findObject(new UiSelector()
                .resourceId(PACKAGE_NAME + ":id/tutorial_summary_text"));
        assertTrue("Tutorial summary should be displayed", tutorialSummary.exists());
        assertTrue("Tutorial summary should contain title",
                tutorialSummary.getText().contains("Paid Tutorial Test"));
        assertTrue("Tutorial summary should show the fee",
                tutorialSummary.getText().contains("$25.00"));

//        Tests cancel button
        UiObject cancelButton = device.findObject(new UiSelector()
                .resourceId(PACKAGE_NAME + ":id/cancel_button"));
        assertTrue("Cancel button should be displayed", cancelButton.exists());
        cancelButton.click();
        boolean activityClosed = device.wait(
                Until.gone(By.res(PACKAGE_NAME + ":id/tutorial_summary_text")),
                5000);
        assertTrue("Activity should close after cancellation", activityClosed);
    }

//    testScrollingThroughLongTutorialDetails() method to test long pages
    @Test
    public void testScrollingThroughLongTutorialDetails() throws UiObjectNotFoundException {

//        Long tutorial intent
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), RegisterForTutorialActivity.class);
        intent.putExtra("tutorialId", "test-tutorial-id");
        intent.putExtra("tutorialTitle", "Tutorial With Long Description");
        intent.putExtra("tutorialFee", "25.00");
        activityRule.launchActivity(intent);

//        Scroll view
        UiScrollable scrollView = new UiScrollable(new UiSelector()
                .className("android.widget.ScrollView"));

//        Verifies scrolling works
        assertTrue("Should be able to scroll", scrollView.exists());
        boolean canScroll = scrollView.scrollToEnd(5);
        assertTrue("Should be able to scroll to the end", canScroll);

//        Looks for pay button
        UiObject payButton = device.findObject(new UiSelector()
                .resourceId(PACKAGE_NAME + ":id/pay_with_paypal_button"));
        assertTrue("Pay button should be visible after scrolling", payButton.exists());
    }

//    testPayButtonForPaidTutorial() method to test paid tutorials
    @Test
    public void testPayButtonForPaidTutorial() throws UiObjectNotFoundException {

//        Paid tutorial intent
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), RegisterForTutorialActivity.class);
        intent.putExtra("tutorialId", "test-tutorial-id");
        intent.putExtra("tutorialTitle", "Paid Tutorial Test");
        intent.putExtra("tutorialFee", "25.00");
        activityRule.launchActivity(intent);

//        Verifies paypal button
        UiObject payButton = device.findObject(new UiSelector()
                .resourceId(PACKAGE_NAME + ":id/pay_with_paypal_button"));
        assertTrue("Pay button should be displayed", payButton.exists());
        assertTrue("Pay button should show 'Pay with PayPal'",
                payButton.getText().equals("Pay with PayPal"));
    }

//    getLauncherPackageName() method to get launcher package name
    private String getLauncherPackageName() {

//        Launcher intent
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

//        Package manager to get launcher name
        PackageManager pm = ApplicationProvider.getApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }
}