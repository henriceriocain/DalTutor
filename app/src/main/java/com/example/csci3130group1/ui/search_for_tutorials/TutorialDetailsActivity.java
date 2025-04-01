package com.example.csci3130group1.ui.search_for_tutorials;

/*import android.content.Intent;
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
}*/



import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.csci3130group1.R;
import com.example.csci3130group1.ReviewActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TutorialDetailsActivity extends AppCompatActivity {

    private String tutorUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_details_activity);

        // Get data
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
        tutorUserId = intent.getStringExtra("tutorUserId"); // 🔑 new line

        // Display
        TextView detailText = findViewById(R.id.tutorial_detail_text);
        String detail = "Topic: " + topic + "\n" +
                "Tutor: " + name + " (" + degree + ")\n" +
                "Fee: $" + fee + "\n" +
                "Duration: " + duration + " mins\n" +
                "Description: " + description + "\n" +
                "Location: " + city + ", " + province + ", " + country;
        detailText.setText(detail);

        // Rate Tutor button
        /*Button rateButton = findViewById(R.id.rate_button);
        rateButton.setOnClickListener(v -> {
            Intent reviewIntent = new Intent(this, ReviewActivity.class);
            reviewIntent.putExtra("reviewedUserId", tutorUserId);
            startActivity(reviewIntent);
        });*/
        Button rateButton = findViewById(R.id.rate_button); // your rate button ID
        rateButton.setOnClickListener(v -> {
            String tutorEmail = getIntent().getStringExtra("email"); // if passed via intent

            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            usersRef.orderByChild("email").equalTo(tutorEmail)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot userSnap : snapshot.getChildren()) {
                                    String tutorUid = userSnap.getKey();

                                    Intent intent = new Intent(TutorialDetailsActivity.this, ReviewActivity.class);
                                    intent.putExtra("reviewedUserId", tutorUid);
                                    startActivity(intent);
                                    break;
                                }
                            } else {
                                Toast.makeText(TutorialDetailsActivity.this, "Tutor not found", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(TutorialDetailsActivity.this, "Error loading tutor info", Toast.LENGTH_SHORT).show();
                        }
                    });
        });


    }
}

