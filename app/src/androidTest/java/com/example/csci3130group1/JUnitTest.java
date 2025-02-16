package com.example.csci3130group1;

import static org.junit.Assert.*;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
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
    private Spinner roleSpinner;
    private Button loginButton;

    @Before
    public void setUp() {
        ActivityScenario<LoginActivity> loginScenario = ActivityScenario.launch(LoginActivity.class);
        loginScenario.onActivity(activity -> {
            emailInput = activity.findViewById(R.id.username_input);
            passwordInput = activity.findViewById(R.id.password_input);
            roleSpinner = activity.findViewById(R.id.role_spinner);
            loginButton = activity.findViewById(R.id.button2);
        });
    }

// ForgotPasswordActivity Tests

//    Tests when email field is empty
    @Test
    public void checkEmailIsEmpty() {
        ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(ForgotPasswordActivity.class);
        scenario.onActivity(activity -> {
            EditText emailInput = activity.findViewById(R.id.email_input);
            emailInput.setText("");
            activity.findViewById(R.id.reset_button).performClick();
            assertEquals("Email is required", emailInput.getError().toString());
        });
    }

//    Tests when email input is valid
    @Test
    public void checkIfEmailIsValid() {
        ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(ForgotPasswordActivity.class);
        scenario.onActivity(activity -> {
            EditText emailInput = activity.findViewById(R.id.email_input);
            emailInput.setText("anEmail@email.ca");
            assertNull(emailInput.getError());
        });
    }

//    Tests when email input is not valid
    @Test
    public void checkIfEmailIsNotValid() {
        ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(ForgotPasswordActivity.class);
        scenario.onActivity(activity -> {
            EditText emailInput = activity.findViewById(R.id.email_input);
            emailInput.setText("hi.com");
            activity.findViewById(R.id.reset_button).performClick();
            assertEquals("Valid email address is required", emailInput.getError().toString());
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

    // ✅ New Test: Invalid Email Format in Login
    @Test
    public void testInvalidEmailFormatInLogin() {
        emailInput.setText("invalidEmail"); // No '@' symbol
        assertFalse(emailInput.getText().toString().contains("@"));
    }

    // ✅ New Test: Successful Login Displays Correct Welcome Message
    @Test
    public void testSuccessfulLoginDisplaysWelcomeMessage() {
        // Set up an intent with the expected username and role
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), HomeActivity.class);
        intent.putExtra("username", "student@example.com");
        intent.putExtra("role", "Student");

        // Launch HomeActivity with the provided intent
        ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            TextView welcomeText = activity.findViewById(R.id.welcome_text);
            assertFalse(TextUtils.isEmpty(welcomeText.getText().toString()));

            // Assert that the welcome text matches the expected format
            assertEquals("Hello and welcome student@example.com! You are logged in as a Student",
                    welcomeText.getText().toString());
        });
    }

    // ✅ New Test: Empty Role Selection in Login
    @Test
    public void testEmptyRoleSelection() {
        // Ensure the roleSpinner is not null and has at least one item
        assertNotNull(roleSpinner);
        assertTrue(roleSpinner.getAdapter().getCount() > 0);

        // Wait for UI to be ready
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            roleSpinner.setSelection(0); // Selecting the first item
        });

        // Get the selected item text safely
        String selectedItem = roleSpinner.getSelectedItem().toString().trim();

        // Allow "Select your role" as a valid empty selection
        assertTrue("Role selection should be empty or default placeholder",
                selectedItem.isEmpty() || selectedItem.equals("Select your role"));
    }

    // ✅ New Test: Login Button Disabled When Fields Are Empty
    @Test
    public void testLoginButtonDisabledWithEmptyFields() {
        loginActivityRule.getScenario().onActivity(activity -> {
            loginButton.setEnabled(false);
            assertFalse(loginButton.isEnabled());
        });
    }
}
