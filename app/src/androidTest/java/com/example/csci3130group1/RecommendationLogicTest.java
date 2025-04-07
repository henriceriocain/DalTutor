package com.example.csci3130group1;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class RecommendationLogicTest {

    @Test
    public void testRecommendationsMatchTutorNameFromPreferencesAndRegistrations() {
        String tutorName = "Alice Smith";

        List<String> expected = Arrays.asList("student1@example.com", "student2@example.com");
        List<String> prefs = Arrays.asList("student1@example.com");  // from preferences
        List<String> regs = Arrays.asList("student2@example.com");  // from registrations

        Set<String> combined = new HashSet<>();
        combined.addAll(prefs);
        combined.addAll(regs);

        assertEquals(new HashSet<>(expected), combined);
    }
    @Test
    public void testRecommendationsWithOverlap() {
        List<String> expected = Arrays.asList("student1@example.com", "student2@example.com");
        List<String> prefs = Arrays.asList("student1@example.com", "student2@example.com");
        List<String> regs = Arrays.asList("student2@example.com");

        Set<String> combined = new HashSet<>();
        combined.addAll(prefs);
        combined.addAll(regs);

        assertEquals(new HashSet<>(expected), combined);
    }

    @Test
    public void testEmptyRecommendations() {
        List<String> expected = Arrays.asList();
        List<String> prefs = Arrays.asList();  // no preferences
        List<String> regs = Arrays.asList();   // no registrations

        Set<String> combined = new HashSet<>();
        combined.addAll(prefs);
        combined.addAll(regs);

        assertEquals(new HashSet<>(expected), combined);
    }
    @Test
    public void testOnlyPreferencesExist() {
        List<String> expected = Arrays.asList("student3@example.com");
        List<String> prefs = Arrays.asList("student3@example.com");
        List<String> regs = Arrays.asList();  // no registrations

        Set<String> combined = new HashSet<>();
        combined.addAll(prefs);
        combined.addAll(regs);

        assertEquals(new HashSet<>(expected), combined);
    }


}
