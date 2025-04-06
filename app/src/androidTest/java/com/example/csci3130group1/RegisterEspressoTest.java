package com.example.csci3130group1;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class RegisterEspressoTest {

    @Rule
    public ActivityScenarioRule<RegisterActivity> activityScenarioRule =
            new ActivityScenarioRule<>(RegisterActivity.class);

    @Test
    public void testUIElementsAreDisplayed() {
        Espresso.onView(withId(R.id.name_input)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(withId(R.id.email_input)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(withId(R.id.password_input)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(withId(R.id.contact_input)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(withId(R.id.role_spinner)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(withId(R.id.register_button)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

}
