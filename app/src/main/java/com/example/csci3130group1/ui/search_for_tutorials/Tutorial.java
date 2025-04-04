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
    private String topic;
    private String fee;
    private String duration;
    private String description;
    private String city;
    private String province;
    private String country;
    private String name;
    private String degree;
    private String userId; // 🔑 Tutor's UID

    // No-argument constructor for Firebase
    public Tutorial() {
    }

    // Full constructor for creating a Tutorial object when publishing
    public Tutorial(String topic, String fee, String duration, String description,
                    String city, String province, String country, String name, String degree, String userId) {
        this.topic = topic;
        this.fee = fee;
        this.duration = duration;
        this.description = description;
        this.city = city;
        this.province = province;
        this.country = country;
        this.name = name;
        this.degree = degree;
        this.userId = userId;
    }

    public Tutorial(String topic, String fee, String duration, String description, String city, String province, String country, String name, String degree) {
    }

    // Getters
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

    public String getUserId() {
        return userId;
    }
}


