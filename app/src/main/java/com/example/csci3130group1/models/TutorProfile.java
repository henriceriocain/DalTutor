package com.example.csci3130group1.models;

public class TutorProfile {
    private String tutorId;
    private String name;
    private String email;
    private String contact;
    private String degree;
    private String description;
    private String profilePictureUrl;
    private int tutorialCount;
    private float averageRating;
    private int reviewCount;

    // No-argument constructor for Firebase
    public TutorProfile() {}

    // Constructor
    public TutorProfile(String tutorId, String name, String email, String contact, 
                       String degree, String description, String profilePictureUrl) {
        this.tutorId = tutorId;
        this.name = name;
        this.email = email;
        this.contact = contact;
        this.degree = degree;
        this.description = description;
        this.profilePictureUrl = profilePictureUrl;
        this.tutorialCount = 0;
        this.averageRating = 0.0f;
        this.reviewCount = 0;
    }

    // Getters
    public String getTutorId() {
        return tutorId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getContact() {
        return contact;
    }

    public String getDegree() {
        return degree;
    }

    public String getDescription() {
        return description;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public int getTutorialCount() {
        return tutorialCount;
    }

    public float getAverageRating() {
        return averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    // Setters
    public void setTutorId(String tutorId) {
        this.tutorId = tutorId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public void setTutorialCount(int tutorialCount) {
        this.tutorialCount = tutorialCount;
    }

    public void setAverageRating(float averageRating) {
        this.averageRating = averageRating;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    // Helper methods
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    public boolean hasProfilePicture() {
        return profilePictureUrl != null && !profilePictureUrl.trim().isEmpty();
    }

    public boolean isQualifiedTutor() {
        return tutorialCount > 0;
    }

    public String getFormattedRating() {
        if (reviewCount > 0) {
            return String.format("%.1f (%d)", averageRating, reviewCount);
        }
        return "No reviews yet";
    }

    public String getTutorialCountText() {
        if (tutorialCount == 1) {
            return "1 tutorial";
        }
        return tutorialCount + " tutorials";
    }
}