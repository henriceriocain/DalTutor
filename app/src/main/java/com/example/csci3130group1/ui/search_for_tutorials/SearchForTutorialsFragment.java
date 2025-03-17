package com.example.csci3130group1.ui.search_for_tutorials;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import com.google.android.gms.location.LocationRequest;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentSearchForTutorialsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchForTutorialsFragment extends Fragment {
    Button btLocation;
    TextView location_text;
    FusedLocationProviderClient client;
    private FragmentSearchForTutorialsBinding binding;
    private ListView tutorialListView;
    private List<Tutorial> tutorialResults;
    private TutorialAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchForTutorialsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        // Initialize UI elements
        tutorialListView = binding.tutorialListView;
        tutorialResults = new ArrayList<>();
        adapter = new TutorialAdapter(requireContext(), tutorialResults);
        tutorialListView.setAdapter(adapter);
        // Ensure the search button does NOT have any android:onClick attribute in XML.
        // Set the click listener programmatically.
        binding.searchButton.setOnClickListener(v -> {
            Log.d(TAG, "Search button clicked");
            performSearch();
        });
        btLocation = root.findViewById(R.id.bt_location);
        client = LocationServices.getFusedLocationProviderClient(getActivity());
        btLocation.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&  ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            getCurrentLocation();
                        }
                        else {
                            requestPermissions(
                                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    100);
                        }
                    }
                }
        );


        // Initialize UI elements
        tutorialListView = binding.tutorialListView;
        tutorialResults = new ArrayList<>();
        adapter = new TutorialAdapter(requireContext(), tutorialResults);
        tutorialListView.setAdapter(adapter);

        // Ensure the search button does NOT have any android:onClick attribute in XML.
        // Set the click listener programmatically.
        binding.searchButton.setOnClickListener(v -> {
            Log.d(TAG, "Search button clicked");
            performSearch();
        });

        return root;
    }

    // Retrieve data from Firebase and then update & filter the tutorials.
    private void performSearch() {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        Log.d(TAG, "Performing search: fetching tutorials from Firebase");

        // Use a single value event listener to fetch data only once.
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tutorialResults.clear();
                Log.d(TAG, "DataSnapshot received with " + snapshot.getChildrenCount() + " items.");

                for (DataSnapshot data : snapshot.getChildren()) {
                    Tutorial tutorial = data.getValue(Tutorial.class);
                    if (tutorial != null) {
                        tutorialResults.add(tutorial);
                        // Use getTopic() if that's your new field name (not getTitle())
                        Log.d(TAG, "Loaded tutorial: " + tutorial.getTopic());
                    }
                }
                Log.d(TAG, "Total tutorials loaded: " + tutorialResults.size());

                // Update the adapter with the newly loaded data.
                adapter.updateTutorials(tutorialResults);

                // Filter the tutorials based on the user inputs.
                filterTutorials();
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load tutorials: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load tutorials", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Retrieve filter criteria from the input fields and apply the filter.
    private void filterTutorials() {
        String locationFilter = binding.locationInput.getText().toString().trim();
        String feeFilter = binding.feeInput.getText().toString().trim();
        String durationFilter = binding.durationInput.getText().toString().trim();

        Log.d(TAG, "Filtering tutorials with location: " + locationFilter +
                ", fee: " + feeFilter + ", duration: " + durationFilter);

        adapter.filter(locationFilter, feeFilter, durationFilter);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && (grantResults.length > 0) && (grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
            getCurrentLocation();
        }
        else {
            Toast.makeText(getActivity(),"Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        Geocoder geocoder = new Geocoder(this.getContext(), Locale.getDefault());
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            client.getLastLocation().addOnCompleteListener(
                    new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            Location location = task.getResult();
                            if (location != null) {
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    String cityName = addresses.get(0).getAddressLine(0);
                                    View root = binding.getRoot();
                                    location_text = root.findViewById(R.id.location_text);
                                    location_text.setText(cityName);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            else {
                                LocationRequest locationRequest = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000).setFastestInterval(1000).setNumUpdates(1);
                                LocationCallback locationCallback = new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        Location location1 = locationResult.getLastLocation();
                                        try {
                                            List<Address> addresses = geocoder.getFromLocation(location1.getLatitude(), location1.getLongitude(), 1);
                                            String cityName = addresses.get(0).getAddressLine(0);
                                            View root = binding.getRoot();
                                            location_text = root.findViewById(R.id.location_text);
                                            location_text.setText(cityName);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                };
                                client.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                            }
                        }
                    }
            );
        }
        else {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}