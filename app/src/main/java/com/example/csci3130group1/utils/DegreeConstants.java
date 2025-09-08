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
    
    // Get degree options for profile editing (no "All Degrees" option)
    public static String[] getProfileDegreeOptions() {
        return DEGREE_OPTIONS;
    }
    
    // Get degree options for filtering (includes "All Degrees" option)
    public static String[] getFilterDegreeOptions() {
        return FILTER_DEGREE_OPTIONS;
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