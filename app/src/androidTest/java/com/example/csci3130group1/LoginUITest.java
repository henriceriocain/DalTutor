package com.example.csci3130group1;

import static androidx.test.espresso.Espresso.onIdle;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static org.hamcrest.Matchers.not;

import android.os.SystemClock;
import androidx.test.espresso.IdlingPolicy;
import java.util.concurrent.TimeUnit;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.espresso.matcher.ViewMatchers;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import android.app.Activity;
import androidx.test.core.app.ActivityScenario;

public class LoginUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> mainActivityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public ActivityScenarioRule<LoginActivity> loginActivityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    private Activity getActivityFromScenario(ActivityScenario<?> scenario) {
        final Activity[] activity = new Activity[1];
        scenario.onActivity(a -> activity[0] = a);
        return activity[0];
    }

    @Test
    public void testLoginButtonNavigatesToLoginActivity() {
        onView(withId(R.id.button2)).perform(click());
        onView(withId(R.id.username_input)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyFieldsShowError() {
        onView(withId(R.id.button2)).perform(click());
        onView(withId(R.id.username_input)).check(matches(withText("")));
        onView(withId(R.id.password_input)).check(matches(withText("")));
    }

    @Test
    public void testInvalidLoginShowsErrorMessage() {
        onView(withId(R.id.username_input)).perform(clearText(), replaceText("wrong@example.com"), closeSoftKeyboard());
        onView(withId(R.id.password_input)).perform(clearText(), replaceText("wrongpass"), closeSoftKeyboard());
        onView(withId(R.id.login_button)).perform(click());
        onIdle();
        loginActivityRule.getScenario().onActivity(activity -> {
            onView(withText("Authentication Failed"))
                    .inRoot(withDecorView(Matchers.not(activity.getWindow().getDecorView())))
                    .check(matches(isDisplayed()));
        });
    }

    @Test
    public void testForgotPasswordNavigatesToResetScreen() {
        onView(withId(R.id.forgot_password)).perform(click());
        onView(withId(R.id.email_input)).check(matches(isDisplayed()));
    }
}
