// Package
package com.example.csci3130group1;

// Import statements
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import org.junit.Rule;
import org.junit.Test;

// ResetPasswordActivityUITest class
public class ResetPasswordActivityUITest {

// Startup for all tests
    @Rule
    public ActivityScenarioRule<ForgotPasswordActivity> activityRule =
            new ActivityScenarioRule<>(ForgotPasswordActivity.class);

//    Tests when theres no input
    @Test
    public void testEmptyEmailShowsError() {
        onView(withId(R.id.reset_button)).perform(click());
        onView(withId(R.id.email_input)).check(matches(hasErrorText("Email is required")));
    }

//    Tests an invalid email
    @Test
    public void testInvalidEmailShowsError() {
        onView(withId(R.id.email_input)).perform(typeText("invalid.email"));
        onView(withId(R.id.reset_button)).perform(click());
        onView(withId(R.id.email_input)).check(matches(hasErrorText("Valid email address is required")));
    }


//    Tests the email field is editable
    @Test
    public void testEmailFieldIsEditable() {
        String testEmail = "anEmail@mail.com";
        onView(withId(R.id.email_input))
                .perform(typeText(testEmail))
                .check(matches(withText(testEmail)));
    }

//    Tests the reset button is visible
    @Test
    public void testResetButtonIsDisplayed() {
        onView(withId(R.id.reset_button))
                .check(matches(isDisplayed()));
    }

//    Tests the "Forgot Password?" text is visible
    @Test
    public void testTitleIsDisplayed() {
        onView(withId(R.id.titleText))
                .check(matches(isDisplayed()))
                .check(matches(withText("Forgot Password?")));
    }
}