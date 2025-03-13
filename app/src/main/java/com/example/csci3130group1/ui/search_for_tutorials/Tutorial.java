package com.example.csci3130group1.ui.search_for_tutorials;
public class Tutorial {
    private String title;
    private String location;
    private String fee;
    private String duration;

    // No-argument constructor for Firebase
    public Tutorial() {}

    public Tutorial(String title, String location, String fee, String duration) {
        this.title = title;
        this.location = location;
        this.fee = fee;
        this.duration = duration;
    }

    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getFee() { return fee; }
    public String getDuration() { return duration; }
}
