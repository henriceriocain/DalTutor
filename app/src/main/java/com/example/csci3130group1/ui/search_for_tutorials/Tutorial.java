package com.example.csci3130group1.ui.search_for_tutorials;
/*public class Tutorial {
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
*/



public class Tutorial {
    private String title;
    private String location;
    private String fee;
    private String duration;
    private String description;
    private String city;
    private String province;
    private String country;
    private String name;
    private String degree;

    // No-argument constructor for Firebase
    public Tutorial() {}

    public Tutorial(String title, String location, String fee, String duration,
                    String description, String city, String province, String country,
                    String name, String degree) {
        this.title = title;
        this.location = location;
        this.fee = fee;
        this.duration = duration;
        this.description = description;
        this.city = city;
        this.province = province;
        this.country = country;
        this.name = name;
        this.degree = degree;
    }

    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getFee() { return fee; }
    public String getDuration() { return duration; }
    public String getDescription() { return description; }
    public String getCity() { return city; }
    public String getProvince() { return province; }
    public String getCountry() { return country; }
    public String getName() { return name; }
    public String getDegree() { return degree; }
}
