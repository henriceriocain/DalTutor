package com.example.csci3130group1.ui.search_for_tutorials;
/*
public class Tutorial {
    private String topic;
    private String fee;
    private String duration;
    private String description;
    private String city;
    private String province;
    private String country;
    private String name;
    private String degree;

    // No-argument constructor for Firebase
    public Tutorial() {
    }

    // DEPRECATED: Legacy constructor - DO NOT USE
    public Tutorial(String topic, String fee, String duration, String description,
                    String city, String province, String country, String name, String degree) {
        // This constructor is deprecated - use the new constructor instead
    }

    // Getters for all fields
    public String getTopic() {
        return topic;
    }

    public String getFee() {
        return fee;
    }

    public String getDuration() {
        return duration;
    }

    public String getDescription() {
        return description;
    }

    public String getCity() {
        return city;
    }

    public String getProvince() {
        return province;
    }

    public String getCountry() {
        return country;
    }

    public String getName() {
        return name;
    }

    public String getDegree() {
        return degree;
    }
}*/


public class Tutorial {
    private String tutorialId; // Firebase key for the tutorial
    private String tutorialName;
    private String topic;
    private String fee;
    private String description;
    private String address; // Full formatted address
    private double latitude;
    private double longitude;
    private String placeId; // Google Places ID for Maps integration
    private String tutorName;
    private String tutorDegree;
    private String tutorId; // 🔑 Tutor's UID
    private String date;
    private String startTime;
    private String endTime;
    
    // Legacy fields for backward compatibility (deprecated)
    private String streetAddress;
    private String postalCode;

    // No-argument constructor for Firebase
    public Tutorial() {
    }

    // Full constructor for creating a Tutorial object when publishing
    public Tutorial(String tutorialName, String topic, String fee, String duration, String description,
                    String streetAddress, String postalCode, String name, String degree, String userId) {
        this.tutorialName = tutorialName;
        this.topic = topic;
        this.fee = fee;
        this.description = description;
        this.streetAddress = streetAddress;
        this.postalCode = postalCode;
        this.tutorName = name;
        this.tutorDegree = degree;
        this.tutorId = userId;
    }

    // Modern constructor with improved field structure
    public Tutorial(String tutorialName, String topic, String fee, String date, String startTime, String endTime,
                    String description, String address, double latitude, double longitude, String placeId, 
                    String tutorName, String tutorId, String tutorDegree) {
        this.tutorialName = tutorialName;
        this.topic = topic;
        this.fee = fee;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeId = placeId;
        this.tutorName = tutorName;
        this.tutorId = tutorId;
        this.tutorDegree = tutorDegree;
        
        // Legacy fields are NOT set - only new clean fields will be written to Firebase
    }
    
    // Legacy constructor for backward compatibility
    public Tutorial(String tutorialName, String topic, String fee, String duration, String description, 
                    String address, double latitude, double longitude, String placeId, String name) {
        this.tutorialName = tutorialName;
        this.topic = topic;
        this.fee = fee;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeId = placeId;
        this.tutorName = name;
    }

    // Legacy constructor for backward compatibility
    public Tutorial(String tutorialName, String topic, String fee, String duration, String description, String streetAddress, String postalCode, String name) {
        this.tutorialName = tutorialName;
        this.topic = topic;
        this.fee = fee;
        this.description = description;
        this.streetAddress = streetAddress;
        this.postalCode = postalCode;
        this.address = streetAddress + ", " + postalCode + ", Halifax, NS"; // Convert to new format
        this.tutorName = name;
    }

    // Getters
    public String getTutorialId() {
        return tutorialId;
    }

    public String getTutorialName() {
        return tutorialName;
    }

    public String getTopic() {
        return topic;
    }

    public String getFee() {
        return fee;
    }


    public String getDescription() {
        return description;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getAddress() {
        return address; // Clean address only
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPlaceId() {
        return placeId;
    }




    
    // New getters for improved structure
    public String getTutorName() {
        return tutorName;
    }
    
    public String getTutorDegree() {
        return tutorDegree;
    }
    
    public String getTutorId() {
        return tutorId;
    }
    
    public String getDate() {
        return date;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }
    
    // Setter for tutorial ID (used when loading from Firebase)
    public void setTutorialId(String tutorialId) {
        this.tutorialId = tutorialId;
    }
}


