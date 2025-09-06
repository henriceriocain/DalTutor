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

    // Full constructor for creating a Tutorial object when publishing
    public Tutorial(String topic, String fee, String duration, String description,
                    String city, String province, String country, String name, String degree) {
        this.topic = topic;
        this.fee = fee;
        this.duration = duration;
        this.description = description;
        this.city = city;
        this.province = province;
        this.country = country;
        this.name = name;
        this.degree = degree;
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
    private String tutorialName;
    private String topic;
    private String fee;
    private String duration;
    private String description;
    private String address; // Full formatted address
    private double latitude;
    private double longitude;
    private String placeId; // Google Places ID for Maps integration
    private String name;
    private String degree;
    private String userId; // 🔑 Tutor's UID
    
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
        this.duration = duration;
        this.description = description;
        this.streetAddress = streetAddress;
        this.postalCode = postalCode;
        this.name = name;
        this.degree = degree;
        this.userId = userId;
    }

    // Modern constructor with location data
    public Tutorial(String tutorialName, String topic, String fee, String duration, String description, 
                    String address, double latitude, double longitude, String placeId, String name) {
        this.tutorialName = tutorialName;
        this.topic = topic;
        this.fee = fee;
        this.duration = duration;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeId = placeId;
        this.name = name;
    }

    // Legacy constructor for backward compatibility
    public Tutorial(String tutorialName, String topic, String fee, String duration, String description, String streetAddress, String postalCode, String name) {
        this.tutorialName = tutorialName;
        this.topic = topic;
        this.fee = fee;
        this.duration = duration;
        this.description = description;
        this.streetAddress = streetAddress;
        this.postalCode = postalCode;
        this.address = streetAddress + ", " + postalCode + ", Halifax, NS"; // Convert to new format
        this.name = name;
    }

    // Getters
    public String getTutorialName() {
        return tutorialName;
    }

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

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getAddress() {
        return address != null ? address : getFullAddress(); // Fallback to legacy format
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

    public String getFullAddress() {
        if (address != null) {
            return address;
        }
        return streetAddress + ", " + postalCode + ", Halifax, NS"; // Legacy fallback
    }

    public String getName() {
        return name;
    }

    public String getDegree() {
        return degree;
    }

    public String getUserId() {
        return userId;
    }
}


