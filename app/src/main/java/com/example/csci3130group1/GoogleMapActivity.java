package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

// GoogleMapsActivity class
public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback {

//    Attributes
    private GoogleMap mMap;
    private DatabaseReference tutorialsRef;
    private Map<Marker, String> markerToTutorialId = new HashMap<>();
    private Marker lastClickedMarker = null;
    private boolean markerClickedOnce = false;
    private Button backButton;

//    onCreate method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);
//        Initializes firebase
        tutorialsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
//        Obtains the SupportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
//        Back button functionality
        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

//    onMapReady method
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

//        Google map variable
        mMap = googleMap;

//        Sets the default location to Dal
        LatLng dalhousie = new LatLng(44.6366, -63.5917);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dalhousie, 15));

//        Loads the tutorials from firebase
        loadTutorialLocations();

//        Marker functionality
//          1st click shows title of tutorial
//          2nd click opens tutorial details page
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.equals(lastClickedMarker)) {
                if (markerClickedOnce) {
//                    Second click state
                    String tutorialId = markerToTutorialId.get(marker);
                    if (tutorialId != null) {
                        navigateToTutorialDetails(tutorialId);
                    }
//                    Reset click state
                    markerClickedOnce = false;
                    lastClickedMarker = null;
                } else {
//                    First click state
                    marker.showInfoWindow();
                    markerClickedOnce = true;
                }
            } else {
//                When another marker is clicked
                marker.showInfoWindow();
                lastClickedMarker = marker;
                markerClickedOnce = true;
            }
            return true;
        });
    }

//    loadTutorialLocations method to get tutorial data from firebase and create markers on maps
    private void loadTutorialLocations() {
        tutorialsRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                Clears markers
                mMap.clear();
                markerToTutorialId.clear();
                lastClickedMarker = null;
                markerClickedOnce = false;

//                    Extracts data from firebase
                for (DataSnapshot tutorialSnapshot : dataSnapshot.getChildren()) {
                    String tutorialId = tutorialSnapshot.getKey();
                    String title = tutorialSnapshot.child("title").getValue(String.class);
                    String location = tutorialSnapshot.child("location").getValue(String.class);
                    LatLng position = getCoordinatesForLocation(location);

//                    Adds markers to map
                    if (position != null) {
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(title));

                        if (marker != null) {
                            markerToTutorialId.put(marker, tutorialId);
                        }
                    }
                }
            }

//            In case that retrieval fails
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(GoogleMapActivity.this,
                        "Failed to load tutorials: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

//    getCoordinatesForLocation method to help with certain keywords for location
    private LatLng getCoordinatesForLocation(String location) {
        if (location == null) return null;
        location = location.toLowerCase();

//        Does not place online tutorials on map
        if (location.contains("online")) {
            return null;
        }

//        Conditionals for certain locations within Dal, defaulting at dal
        if (location.contains("killam") || location.contains("library")) {
            return new LatLng(44.6372, -63.5929);
        } else if (location.contains("cs") || location.contains("computer science")) {
            return new LatLng(44.6376, -63.5876);
        } else if (location.contains("mccain")) {
            return new LatLng(44.6371, -63.5946);
        } else if (location.contains("sub") || location.contains("student union")) {
            return new LatLng(44.6356, -63.5923);
        } else if (location.contains("lsc") || location.contains("life science")) {
            return new LatLng(44.6366, -63.5937);
        } else {
            double lat = 44.6366 + (Math.random() - 0.5) * 0.003;
            double lng = -63.5917 + (Math.random() - 0.5) * 0.003;
            return new LatLng(lat, lng);
        }
    }

//    navigateToTutorialDetails method to go to tutorial details page
    private void navigateToTutorialDetails(String tutorialId) {
        Intent intent = new Intent(GoogleMapActivity.this, TutorialDetailsActivity.class);
        intent.putExtra("tutorialId", tutorialId);
        startActivity(intent);
    }
}
