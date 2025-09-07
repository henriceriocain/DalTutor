package com.example.csci3130group1.models;

import java.util.HashMap;
import java.util.Map;

public class TutorialSession {
    private String tutorialId; // Firebase key
    private String address;
    private String date;
    private String description;
    private String endTime;
    private String fee;
    private Double latitude;
    private Double longitude;
    private String placeId;
    private Map<String, Boolean> registeredStudents;
    private String startTime;
    private String topic;
    private String tutorId;
    private String tutorName;
    private String tutorialName;

    // No-argument constructor for Firebase
    public TutorialSession() {
        this.registeredStudents = new HashMap<>();
    }

    // Full constructor
    public TutorialSession(String address, String date, String description, String endTime, 
                          String fee, Double latitude, Double longitude, String placeId,
                          String startTime, String topic, String tutorId, String tutorName, 
                          String tutorialName) {
        this.address = address;
        this.date = date;
        this.description = description;
        this.endTime = endTime;
        this.fee = fee;
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeId = placeId;
        this.startTime = startTime;
        this.topic = topic;
        this.tutorId = tutorId;
        this.tutorName = tutorName;
        this.tutorialName = tutorialName;
        this.registeredStudents = new HashMap<>();
    }

    // Getters
    public String getTutorialId() {
        return tutorialId;
    }

    public String getAddress() {
        return address;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getFee() {
        return fee;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getPlaceId() {
        return placeId;
    }

    public Map<String, Boolean> getRegisteredStudents() {
        return registeredStudents != null ? registeredStudents : new HashMap<>();
    }

    public String getStartTime() {
        return startTime;
    }

    public String getTopic() {
        return topic;
    }

    public String getTutorId() {
        return tutorId;
    }

    public String getTutorName() {
        return tutorName;
    }

    public String getTutorialName() {
        return tutorialName;
    }

    // Setters
    public void setTutorialId(String tutorialId) {
        this.tutorialId = tutorialId;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public void setRegisteredStudents(Map<String, Boolean> registeredStudents) {
        this.registeredStudents = registeredStudents;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setTutorId(String tutorId) {
        this.tutorId = tutorId;
    }

    public void setTutorName(String tutorName) {
        this.tutorName = tutorName;
    }

    public void setTutorialName(String tutorialName) {
        this.tutorialName = tutorialName;
    }

    // Helper methods
    public boolean hasRegisteredStudents() {
        return registeredStudents != null && !registeredStudents.isEmpty();
    }

    public int getRegisteredStudentCount() {
        return registeredStudents != null ? registeredStudents.size() : 0;
    }

    public boolean isStudentRegistered(String studentId) {
        return registeredStudents != null && 
               registeredStudents.containsKey(studentId) && 
               Boolean.TRUE.equals(registeredStudents.get(studentId));
    }

    // Convert to legacy Tutorial object for backward compatibility
    public com.example.csci3130group1.ui.search_for_tutorials.Tutorial toTutorial() {
        com.example.csci3130group1.ui.search_for_tutorials.Tutorial tutorial = 
            new com.example.csci3130group1.ui.search_for_tutorials.Tutorial(
                this.tutorialName != null ? this.tutorialName : "Unnamed Tutorial",
                this.topic != null ? this.topic : "General",
                this.fee != null ? this.fee : "0",
                this.date != null ? this.date : "TBD",
                this.startTime != null ? this.startTime : "TBD",
                this.endTime != null ? this.endTime : "TBD",
                this.description != null ? this.description : "No description available",
                this.address != null ? this.address : "Location TBD",
                this.latitude != null ? this.latitude : 0.0,
                this.longitude != null ? this.longitude : 0.0,
                this.placeId != null ? this.placeId : "",
                this.tutorName != null ? this.tutorName : "Unknown Tutor",
                this.tutorId != null ? this.tutorId : "",
                ""  // tutorDegree - not available in TutorialSession
            );
        tutorial.setTutorialId(this.tutorialId);
        return tutorial;
    }
}