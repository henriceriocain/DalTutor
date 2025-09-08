package com.example.csci3130group1.utils;

import android.content.Context;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;

import com.example.csci3130group1.adapters.LocationDropdownAdapter;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocationSpinnerUtils {
    private static final String TAG = "LocationSpinnerUtils";
    
    public static class DalPlace {
        public String campus;
        public String name;
        public String addr;
        public double lat;
        public double lon;
        
        public DalPlace() {}
        
        public DalPlace(String campus, String name, String addr, double lat, double lon) {
            this.campus = campus;
            this.name = name;
            this.addr = addr;
            this.lat = lat;
            this.lon = lon;
        }
        
        public String toLabel() { 
            return name + " • " + campus + "\n" + addr; 
        }
        
        public String getDisplayName() {
            String streetAddress = addr.split(",")[0];
            return name + " - " + streetAddress;
        }
        
        public LatLng getLatLng() {
            return new LatLng(lat, lon);
        }
        
        public String getPlaceId() {
            return "dal:" + name.replace(" ", "_");
        }
    }
    
    public interface LocationSelectionListener {
        void onLocationSelected(DalPlace place);
        void onLocationCleared();
    }
    
    public static List<DalPlace> loadDalPlaces(Context context) {
        try (InputStream is = context.getAssets().open("dal_locations.json")) {
            byte[] buf = new byte[is.available()];
            is.read(buf);
            String json = new String(buf, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            List<DalPlace> list = new ArrayList<>();
            
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                DalPlace p = new DalPlace();
                p.campus = o.getString("campus");
                p.name = o.getString("name");
                p.addr = o.getString("addr");
                p.lat = o.getDouble("lat");
                p.lon = o.getDouble("lon");
                list.add(p);
            }
            
            Log.d(TAG, "Loaded " + list.size() + " Dal places from JSON");
            return list;
        } catch (Exception e) {
            Log.e(TAG, "Error loading Dal places", e);
            return Collections.emptyList();
        }
    }
    
    public static void setupSpinner(Context context, Spinner locationSpinner, 
                                  List<DalPlace> places, LocationSelectionListener listener) {
        Log.d(TAG, "Setting up location Spinner with " + places.size() + " places");
        
        // Create simple labels for spinner (since Spinner doesn't support complex layouts as well)
        List<String> locationLabels = new ArrayList<>();
        locationLabels.add("Select a location...");
        
        for (DalPlace place : places) {
            locationLabels.add(place.getDisplayName());
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, locationLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);
        
        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0) {
                    DalPlace selectedPlace = places.get(position - 1);
                    if (listener != null) {
                        listener.onLocationSelected(selectedPlace);
                    }
                } else {
                    if (listener != null) {
                        listener.onLocationCleared();
                    }
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        Log.d(TAG, "Location Spinner setup completed");
    }
    
    public static void setupAutoCompleteTextView(Context context, AutoCompleteTextView locationFilter, 
                                               List<DalPlace> places, LocationSelectionListener listener) {
        Log.d(TAG, "Setting up location AutoCompleteTextView with " + places.size() + " places");
        
        LocationDropdownAdapter adapter = new LocationDropdownAdapter(context, places, true);
        locationFilter.setAdapter(adapter);
        
        // Set dropdown height for better visibility
        locationFilter.setDropDownHeight(600);
        
        locationFilter.setOnItemClickListener((parent, view, position, id) -> {
            Object item = adapter.getItem(position);
            
            if (item instanceof String && "All Locations".equals(item)) {
                // "All Locations" selected
                if (listener != null) {
                    listener.onLocationCleared();
                }
            } else if (item instanceof DalPlace) {
                // Specific location selected
                DalPlace selectedPlace = (DalPlace) item;
                if (listener != null) {
                    listener.onLocationSelected(selectedPlace);
                }
            }
        });
        
        Log.d(TAG, "Location AutoCompleteTextView setup completed");
    }
    
    public static boolean isLocationInHalifax(LatLng latLng) {
        if (latLng == null) return false;
        
        double minLat = 44.6;
        double maxLat = 44.7;
        double minLng = -63.7;
        double maxLng = -63.5;
        
        return latLng.latitude >= minLat && latLng.latitude <= maxLat &&
               latLng.longitude >= minLng && latLng.longitude <= maxLng;
    }
    
    public static boolean isValidHalifaxPostalCode(String postalCode) {
        String[] halifaxPrefixes = {"B3H", "B3J", "B3K", "B3L", "B3M", "B3N", "B3P", "B3S", "B3T"};
        String cleanPostal = postalCode.toUpperCase().replaceAll("\\s", "");
        
        if (cleanPostal.length() < 3) return false;
        
        String prefix = cleanPostal.substring(0, 3);
        for (String validPrefix : halifaxPrefixes) {
            if (prefix.equals(validPrefix)) return true;
        }
        return false;
    }
}