package com.example.csci3130group1;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class US11_Espresso {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule = new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testManagePreferencesEspresso() {

        onView(withId(R.id.username_input)).perform(typeText("gv749789@dal.ca"));
        closeSoftKeyboard();

        onView(withId(R.id.password_input)).perform(typeText("Gavin26672!"));
        closeSoftKeyboard();

        onView(withId(R.id.role_spinner)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("Student"))).perform(click());
        onView(withId(R.id.role_spinner)).check(matches(withSpinnerText("Student")));

        onView(withId(R.id.button2)).perform(click());

        SystemClock.sleep(3000);

        onView(withText("Manage Preferences")).perform(click());

        onView(withId(R.id.CS)).perform(click());

        onView(withId(R.id.tutor)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("Gavin Rainnie"))).perform(click());
        onView(withId(R.id.tutor)).check(matches(withSpinnerText("Gavin Rainnie")));

        onView(withId(R.id.savepref)).perform(click());
    }
}
