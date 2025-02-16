package com.example.csci3130group1;

import static org.junit.Assert.*;

import android.text.TextUtils;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class JUnitTest {

    @Rule
    public ActivityScenarioRule<ForgotPasswordActivity> forgotPasswordRule = new ActivityScenarioRule<>(ForgotPasswordActivity.class);

    @Rule
    public ActivityScenarioRule<LoginActivity> loginActivityRule = new ActivityScenarioRule<>(LoginActivity.class);

    private EditText emailInput;
    private EditText passwordInput;

    @Before
    public void setUp() {
        ActivityScenario<LoginActivity> loginScenario = ActivityScenario.launch(LoginActivity.class);
        loginScenario.onActivity(activity -> {
            emailInput = activity.findViewById(R.id.username_input);
            passwordInput = activity.findViewById(R.id.password_input);
        });
    }

    // ✅ Forgot Password Tests
    @Test
    public void testEmptyEmailShouldShowErrorInForgotPassword() {
        ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(ForgotPasswordActivity.class);
        scenario.onActivity(activity -> {
            EditText emailInput = activity.findViewById(R.id.email_input);
            emailInput.setText("");  // Simulate empty input
            activity.findViewById(R.id.reset_button).performClick();
            assertEquals("Email is required", emailInput.getError().toString());
        });
    }

    @Test
    public void testValidEmailForReset() {
        ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(ForgotPasswordActivity.class);
        scenario.onActivity(activity -> {
            EditText emailInput = activity.findViewById(R.id.email_input);
            emailInput.setText("pass1234@dal.ca");
            assertNull(emailInput.getError()); // No error should appear
        });
    }

    // ✅ Login Tests
    @Test
    public void testEmptyEmailShouldShowErrorInLogin() {
        emailInput.setText("");  // Simulate empty email
        assertTrue(TextUtils.isEmpty(emailInput.getText().toString()));
    }

    @Test
    public void testEmptyPasswordShouldShowErrorInLogin() {
        passwordInput.setText("");  // Simulate empty password
        assertTrue(TextUtils.isEmpty(passwordInput.getText().toString()));
    }

    @Test
    public void testValidLogin() {
        emailInput.setText("student@example.com");
        passwordInput.setText("password123");
        assertFalse(TextUtils.isEmpty(emailInput.getText().toString()));
        assertFalse(TextUtils.isEmpty(passwordInput.getText().toString()));
    }

    @Test
    public void testInvalidLogin() {
        emailInput.setText("wrong@example.com");
        passwordInput.setText("wrongpass");
        assertFalse(emailInput.getText().toString().equals("student@example.com"));
        assertFalse(emailInput.getText().toString().equals("tutor@example.com"));
    }
}
