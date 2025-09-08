package com.example.csci3130group1.utils;

public class DegreeConstants {
    
    // Standardized degree options for consistent use across the app
    public static final String[] DEGREE_OPTIONS = {
        "Arts",
        "Science", 
        "Computer Science",
        "Engineering",
        "Business / Commerce",
        "Economics",
        "Psychology",
        "Law",
        "Medicine",
        "Dentistry",
        "Nursing",
        "Pharmacy",
        "Health Professions",
        "Social Work",
        "Agriculture",
        "Architecture",
        "Planning"
    };
    
    // Options for filter dropdowns (includes "All Degrees" option)
    public static final String[] FILTER_DEGREE_OPTIONS = {
        "All Degrees",
        "Arts",
        "Science", 
        "Computer Science",
        "Engineering",
        "Business / Commerce",
        "Economics",
        "Psychology",
        "Law",
        "Medicine",
        "Dentistry",
        "Nursing",
        "Pharmacy",
        "Health Professions",
        "Social Work",
        "Agriculture",
        "Architecture",
        "Planning"
    };
    
    // Get degree options for profile editing (with empty state)
    public static String[] getProfileDegreeOptions() {
        String[] profileOptions = new String[DEGREE_OPTIONS.length + 1];
        profileOptions[0] = "Select your degree (Optional)";
        System.arraycopy(DEGREE_OPTIONS, 0, profileOptions, 1, DEGREE_OPTIONS.length);
        return profileOptions;
    }
    
    // Get degree options for filtering (includes "All Degrees" option)
    public static String[] getFilterDegreeOptions() {
        return FILTER_DEGREE_OPTIONS;
    }
    
    // Get degree options for tutor search filtering (includes "All Degrees" and "No Degree Listed")
    public static String[] getTutorSearchDegreeOptions() {
        String[] tutorOptions = new String[FILTER_DEGREE_OPTIONS.length + 1];
        tutorOptions[0] = "All Degrees";
        tutorOptions[1] = "No Degree Listed";
        System.arraycopy(DEGREE_OPTIONS, 0, tutorOptions, 2, DEGREE_OPTIONS.length);
        return tutorOptions;
    }
    
    // Check if a degree string is valid
    public static boolean isValidDegree(String degree) {
        if (degree == null || degree.trim().isEmpty()) {
            return false;
        }
        
        for (String validDegree : DEGREE_OPTIONS) {
            if (validDegree.equalsIgnoreCase(degree.trim())) {
                return true;
            }
        }
        return false;
    }
}