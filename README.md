# Best Practices To Follow for Term Project

## Recommended Android Project Configurations

1. Android Studio Koala — 2024.1.1 Patch 2
2. Android Gradle Plugin 8.5.2. [Learn about AGP compatibility.](https://developer.android.com/build/releases/gradle-plugin)
3. Android API Level 34
4. Emulator: API Level 35 + Pixel 2
5. JDK 17
6. Language - Java

## Git Guidelines to Avoid Merge Conflicts

To ensure smooth collaboration and minimize the risk of merge conflicts, please follow the Git guidelines outlined below for your project workflow:

1. __Create a new project__ and push it to the ```main or master``` branch (this is a one-time activity. Make sure ```main or master``` is set as the default branch in the repository created for you).
2. __Create the ```dev``` branch__ from the ```main or master``` branch (one-time activity).
3. __Clone the project__ to your local environment (e.g., Android Studio).
4. __Create a feature branch__ for your assigned task (You can also create the branch directly through the GitLab web interface).
5. __Switch to your feature branch__ before making any changes.
6. __Make changes locally__ and commit your work regularly.
7. __Merge the ```dev``` branch__ into your feature branch to ensure it is up to date with the latest changes (this can be done locally using ```git merge dev```. Make sure you have pulled the latest version of the ```dev``` branch locally and are on your feature branch before merging).
8. __Push your commits__ to the remote feature branch.
9. __Create a merge request (MR)__ from your feature branch to the ```dev``` branch.
10. __Request a code review__ from your pair programmer.
11. After approval, __merge the MR__ into the ```dev``` branch.
12. Create a merge request (MR) from ```dev``` branch to ```main or master``` branch at the end of iteration.

## Simplify Merge Request Description with Templates in Gitlab

1. Creating the Template in GitLab
2. Create Markdown File
3. Start by creating a Markdown file that will serve as your merge request description template. You can use any text editor to create the file under .gitlab/merge_request_templates inside your project directory.

This is sample templates:

.gitlab_merge_request_templates/Default.md

```
### Description
This merge request addresses, and describe the problem or user story being addressed.

### Changes Made
Provide code snippets or screenshots as needed.

### Related Issues
Provide links to the related issues or feature requests.

### Additional Notes
Include any extra information or considerations for reviewers, such as impacted areas of the codebase.

### Merge Request Checklists
- [ ] Code follows project coding guidelines.
- [ ] Documentation reflects the changes made.
- [ ] I have already covered the unit testing.
```

Please follow [this blog](https://medium.com/gravel-engineering/simplify-merge-request-description-with-templates-in-gitlab-45dca182185d) for reference.

## Commit Guidelines

Using a clear and consistent commit message structure is key when following Test-Driven Development (TDD). Here’s how to craft commit titles at different stages of the TDD cycle - failed test, implementation, and refactoring.

### 1. Commit Title for Writing a Failing Test (Red Phase)

This stage involves writing a new test for the desired functionality that will initially fail because the feature is not yet implemented.

Example Commit Title:
```
test: add failing test for [feature/behavior]
```
Example:
```
test: add failing test for login validation
```

### 2. Commit Title for Implementing Code to Pass the Test (Green Phase)

At this stage, you write the minimal amount of code needed to pass the failing test. The focus is on making the test pass, without worrying too much about optimization or refactoring.

Example Commit Title:
```
feat: implement [feature/behavior] to pass test
```

Example:
```
feat: implement login validation logic to pass test
```

### 3. Commit Title for Refactoring (Refactor Phase)

Once the test is passing, you refactor the code for optimization, better readability, or improved design, without changing the functionality. The tests should continue to pass after refactoring.

Example Commit Title:
```
refactor: improve [module/class/method] after passing test
```

Example:
```
refactor: clean up login validation logic for better readability
```

### Full Example in Context (Login Validation Feature)
Failing Test (Red Phase):
```
// LoginViewModelTest.java

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LoginViewModelTest {

    private LoginViewModel loginViewModel;

    @Before
    public void setUp() {
        loginViewModel = new LoginViewModel();
    }

    @Test
    public void login_shouldReturnFalse_whenUsernameIsEmpty() {
        boolean result = loginViewModel.login("", "password123");
        assertFalse(result);
    }

    @Test
    public void login_shouldReturnFalse_whenPasswordIsEmpty() {
        boolean result = loginViewModel.login("user", "");
        assertFalse(result);
    }
}
```
Commit Title for the Failing Test:
```
test: add failing test for login validation with empty fields
```
Implementation (Green Phase):
```
// LoginViewModel.java

public class LoginViewModel {

    public boolean login(String username, String password) {
        return !username.isEmpty() && !password.isEmpty();
    }
}
```
Commit Title for the Implementation:
```
feat: implement basic login validation for empty username and password
```
Refactoring (Refactor Phase):
```
// LoginViewModel.java

public class LoginViewModel {

    public boolean login(String username, String password) {
        return isValidInput(username, password);
    }

    private boolean isValidInput(String username, String password) {
        return !username.isEmpty() && !password.isEmpty();
    }
}
```
Commit Title for the Refactor:
```
refactor: move validation logic to separate method for better maintainability
```
These commit titles help keep the commit history organized, making it easier to track the progression of work, particularly in a collaborative team or Agile environment.

## Steps to Set Push Rules for Commit Messages in GitLab

1. Navigate to Your GitLab Project:
    
    Open your GitLab project, and on the left-hand menu, go to __Settings > Repository__.

2. Locate the Push Rules Section:

    Scroll down to the Push Rules section.

3. Set a Commit Message Regular Expression:

    In the Commit message field, you can specify a regular expression (regex) that the commit message must match in order to be accepted.

For example, to enforce that all commit messages must start with one of the prefixes test:, feat:, or refactor:, you can use the following regex:

```
^(test|feat|refactor): .*
```
This will enforce that commit messages:

Start with test:, feat:, or refactor:
Have a space after the colon and then the message content.

4. Set Other Rules (Optional):

    You can also configure additional push rules, such as preventing pushes to specific branches or restricting file types that can be committed.

5. Save Changes:

    After configuring the commit message regex, click Save changes at the bottom of the page.

Other Example Push Rule Configuration

```
^(test|feat|fix|refactor): .{10,}
```
This regex enforces:

- A commit message that starts with one of the specified prefixes (test:, feat:, fix:, or refactor:).
- A commit message that is at least 10 characters long after the prefix and space.
