package com.example.csci3130group1;
import com.example.csci3130group1.TutorialDetailsActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.example.csci3130group1.utils.TutorialTimeUtils;
import com.example.csci3130group1.adapters.LocationDashboardAdapter;

// DAL Location data class
class DalLocation {
    public final String campus;
    public final String name;
    public final String addr;
    public final double lat;
    public final double lon;

    public DalLocation(String campus, String name, String addr, double lat, double lon) {
        this.campus = campus;
        this.name = name;
        this.addr = addr;
        this.lat = lat;
        this.lon = lon;
    }

    public String getPlaceId() {
        return "dal:" + name.replace(" ", "_");
    }
}

// GoogleMapsActivity class
public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback {

//    Attributes
    private GoogleMap mMap;
    private DatabaseReference tutorialsRef;
    private Map<Marker, String> markerToTutorialId = new HashMap<>();
    private Map<Marker, String> markerToBuildingName = new HashMap<>();
    private Map<Marker, String> markerToPlaceId = new HashMap<>();
    private List<DalLocation> dalLocations = new ArrayList<>();
    private Marker lastClickedMarker = null;
    private boolean markerClickedOnce = false;
    
    // Dashboard components
    private MaterialCardView bottomDashboard;
    private FloatingActionButton fabShowDashboard;
    private RecyclerView locationsRecycler;
    private LinearLayout emptyStateLayout;
    private LocationDashboardAdapter dashboardAdapter;
    private List<LocationDashboardAdapter.LocationInfo> locationInfoList = new ArrayList<>();

//    onCreate() method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);
//        Load DAL locations from JSON
        loadDalLocations();
//        Initializes firebase
        tutorialsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
//        Initialize dashboard components
        initializeDashboard();
    }

//    onMapReady() method
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
//          1st click shows building name
//          2nd click navigates to location tutorials page
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.equals(lastClickedMarker)) {
                if (markerClickedOnce) {
//                    Second click state - navigate to location tutorials
                    String buildingName = markerToBuildingName.get(marker);
                    String placeId = markerToPlaceId.get(marker);
                    if (buildingName != null && placeId != null) {
                        navigateToLocationTutorials(buildingName, placeId);
                    }
//                    Reset click state
                    markerClickedOnce = false;
                    lastClickedMarker = null;
                } else {
//                    First click state - show building info
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

//    loadTutorialLocations() method to get tutorial data from firebase and create markers on maps
    private void loadTutorialLocations() {
        tutorialsRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

//                Clears all markers
                mMap.clear();
                markerToTutorialId.clear();
                markerToBuildingName.clear();
                markerToPlaceId.clear();
                lastClickedMarker = null;
                markerClickedOnce = false;

//                Group tutorials by location using placeId
                Map<String, List<String>> tutorialsByLocation = new HashMap<>();
                Map<String, String> tutorialTitles = new HashMap<>();

                long now = System.currentTimeMillis();
                for (DataSnapshot tutorialSnapshot : dataSnapshot.getChildren()) {
                    String tutorialId = tutorialSnapshot.getKey();
                    String placeId = tutorialSnapshot.child("placeId").getValue(String.class);

//                    Skip online tutorials or tutorials without placeId
                    if (placeId == null || !placeId.startsWith("dal:")) {
                        continue;
                    }

//                    Only include tutorials that have not ended yet
                    boolean isUpcoming = false;
                    Long endTimestamp = tutorialSnapshot.child("endTimestamp").getValue(Long.class);
                    if (endTimestamp != null) {
                        isUpcoming = endTimestamp >= now;
                    } else {
                        String date = tutorialSnapshot.child("date").getValue(String.class);
                        String endTime = tutorialSnapshot.child("endTime").getValue(String.class);
                        isUpcoming = TutorialTimeUtils.isUpcoming(date, endTime, now);
                    }
                    if (!isUpcoming) {
                        continue;
                    }

//                    Get tutorial title
                    String title = tutorialSnapshot.child("tutorialName").getValue(String.class);
                    if (title == null) {
                        title = tutorialSnapshot.child("topic").getValue(String.class);
                    }
                    tutorialTitles.put(tutorialId, title != null ? title : "Unknown Tutorial");

//                    Group tutorials by location
                    if (!tutorialsByLocation.containsKey(placeId)) {
                        tutorialsByLocation.put(placeId, new ArrayList<>());
                    }
                    tutorialsByLocation.get(placeId).add(tutorialId);
                }

//                Create markers only for locations that have tutorials
                for (DalLocation location : dalLocations) {
                    String placeId = location.getPlaceId();
                    List<String> tutorialIds = tutorialsByLocation.get(placeId);

                    if (tutorialIds != null && !tutorialIds.isEmpty()) {
                        LatLng position = new LatLng(location.lat, location.lon);
                        
//                        Create marker title showing building name and tutorial count
                        String markerTitle = location.name;
                        if (tutorialIds.size() > 1) {
                            markerTitle += " (" + tutorialIds.size() + " tutorials)";
                        }

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(markerTitle)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                        if (marker != null) {
//                            Store the first tutorial ID for single-tutorial navigation
                            markerToTutorialId.put(marker, tutorialIds.get(0));
                            markerToBuildingName.put(marker, location.name);
                            markerToPlaceId.put(marker, placeId);
                        }
                    }
                }

//                Update dashboard with current tutorial data
                updateDashboard(tutorialsByLocation);
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

//    loadDalLocations() method to load predefined locations from JSON
    private void loadDalLocations() {
        try {
            InputStream inputStream = getAssets().open("dal_locations.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, "UTF-8");
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject locationJson = jsonArray.getJSONObject(i);
                
                String campus = locationJson.getString("campus");
                String name = locationJson.getString("name");
                String addr = locationJson.getString("addr");
                double lat = locationJson.getDouble("lat");
                double lon = locationJson.getDouble("lon");

                dalLocations.add(new DalLocation(campus, name, addr, lat, lon));
            }

        } catch (IOException | JSONException e) {
            Toast.makeText(this, "Failed to load DAL locations: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

//    navigateToLocationTutorials method to go to location tutorials page
    private void navigateToLocationTutorials(String locationName, String placeId) {
        Intent intent = new Intent(GoogleMapActivity.this, LocationTutorialsActivity.class);
        intent.putExtra("locationName", locationName);
        intent.putExtra("placeId", placeId);
        startActivity(intent);
    }

//    navigateToTutorialDetails method to go to tutorial details page (kept for backward compatibility)
    private void navigateToTutorialDetails(String tutorialId) {
        Intent intent = new Intent(GoogleMapActivity.this, TutorialDetailsActivity.class);
        intent.putExtra("tutorialId", tutorialId);
        startActivity(intent);
    }

//    Initialize dashboard components and functionality
    private void initializeDashboard() {
        bottomDashboard = findViewById(R.id.bottomDashboard);
        fabShowDashboard = findViewById(R.id.fabShowDashboard);
        locationsRecycler = findViewById(R.id.locationsRecycler);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        
        // Setup RecyclerView
        locationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        dashboardAdapter = new LocationDashboardAdapter(locationInfoList, this::navigateToLocationFromDashboard);
        locationsRecycler.setAdapter(dashboardAdapter);
        
        // FAB click listener
        fabShowDashboard.setOnClickListener(v -> showDashboard());
        
        // Close dashboard button
        findViewById(R.id.btnCloseDashboard).setOnClickListener(v -> hideDashboard());
    }

//    Show dashboard with animation
    private void showDashboard() {
        if (bottomDashboard.getVisibility() == View.VISIBLE) return;
        
        bottomDashboard.setVisibility(View.VISIBLE);
        bottomDashboard.setTranslationY(bottomDashboard.getHeight());
        bottomDashboard.animate()
            .translationY(0)
            .setDuration(300)
            .start();
            
        fabShowDashboard.hide();
    }

//    Hide dashboard with animation
    private void hideDashboard() {
        bottomDashboard.animate()
            .translationY(bottomDashboard.getHeight())
            .setDuration(300)
            .withEndAction(() -> {
                bottomDashboard.setVisibility(View.GONE);
                fabShowDashboard.show();
            })
            .start();
    }

//    Navigate to location tutorials from dashboard
    private void navigateToLocationFromDashboard(LocationDashboardAdapter.LocationInfo location) {
        navigateToLocationTutorials(location.locationName, location.placeId);
        hideDashboard();
    }

//    Update dashboard with current location data
    private void updateDashboard(Map<String, List<String>> tutorialsByLocation) {
        locationInfoList.clear();
        
        for (DalLocation location : dalLocations) {
            String placeId = location.getPlaceId();
            List<String> tutorialIds = tutorialsByLocation.get(placeId);
            
            if (tutorialIds != null && !tutorialIds.isEmpty()) {
                locationInfoList.add(new LocationDashboardAdapter.LocationInfo(
                    location.name, placeId, tutorialIds.size()));
            }
        }
        
        dashboardAdapter.updateLocations(locationInfoList);
        
        // Show/hide empty state
        if (locationInfoList.isEmpty()) {
            locationsRecycler.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            locationsRecycler.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
}
