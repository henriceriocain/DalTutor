package com.example.csci3130group1.ui.search_for_tutorials;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.csci3130group1.R;

public class TutorialDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_details_activity);

        // Retrieve the data passed via the Intent
        Intent intent = getIntent();
        String topic = intent.getStringExtra("topic");
        String fee = intent.getStringExtra("fee");
        String duration = intent.getStringExtra("duration");
        String description = intent.getStringExtra("description");
        String city = intent.getStringExtra("city");
        String province = intent.getStringExtra("province");
        String country = intent.getStringExtra("country");
        String name = intent.getStringExtra("name");
        String degree = intent.getStringExtra("degree");

        // Find the TextView in your layout to display detailed tutorial information.
        TextView detailText = findViewById(R.id.tutorial_detail_text);

        // Build a detailed description string.
        String detail = "Topic: " + topic + "\n" +
                "Tutor: " + name + " (" + degree + ")\n" +
                "Fee: $" + fee + "\n" +
                "Duration: " + duration + " mins\n" +
                "Description: " + description + "\n" +
                "Location: " + city + ", " + province + ", " + country;

        detailText.setText(detail);
    }
}
