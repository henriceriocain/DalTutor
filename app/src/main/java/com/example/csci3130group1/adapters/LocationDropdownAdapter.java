package com.example.csci3130group1.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.csci3130group1.R;
import com.example.csci3130group1.utils.LocationSpinnerUtils;

import java.util.ArrayList;
import java.util.List;

public class LocationDropdownAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private List<LocationSpinnerUtils.DalPlace> allPlaces;
    private List<LocationSpinnerUtils.DalPlace> filteredPlaces;
    private boolean hasAllLocationsOption;
    private LayoutInflater inflater;

    public LocationDropdownAdapter(Context context, List<LocationSpinnerUtils.DalPlace> places, boolean hasAllLocationsOption) {
        this.context = context;
        this.allPlaces = new ArrayList<>(places);
        this.filteredPlaces = new ArrayList<>(places);
        this.hasAllLocationsOption = hasAllLocationsOption;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return filteredPlaces.size() + (hasAllLocationsOption ? 1 : 0);
    }

    @Override
    public Object getItem(int position) {
        if (hasAllLocationsOption && position == 0) {
            return "All Locations";
        }
        int adjustedPosition = hasAllLocationsOption ? position - 1 : position;
        return filteredPlaces.get(adjustedPosition);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        
        if (hasAllLocationsOption && position == 0) {
            // Simple view for "All Locations"
            if (view == null || view.findViewById(R.id.location_name) != null) {
                view = inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }
            TextView textView = (TextView) view;
            textView.setText("All Locations");
            textView.setTextSize(16);
            textView.setPadding(24, 16, 24, 16);
            return view;
        }

        // Custom view for location places
        if (view == null || view.findViewById(R.id.location_name) == null) {
            view = inflater.inflate(R.layout.location_dropdown_item, parent, false);
        }

        int adjustedPosition = hasAllLocationsOption ? position - 1 : position;
        LocationSpinnerUtils.DalPlace place = filteredPlaces.get(adjustedPosition);

        TextView locationName = view.findViewById(R.id.location_name);
        TextView locationDetails = view.findViewById(R.id.location_details);

        locationName.setText(place.name);
        
        // Extract street address for cleaner display
        String streetAddress = place.addr.split(",")[0];
        locationDetails.setText(place.campus + " • " + streetAddress);

        return view;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<LocationSpinnerUtils.DalPlace> filtered = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(allPlaces);
                } else {
                    String query = constraint.toString().toLowerCase().trim();
                    
                    for (LocationSpinnerUtils.DalPlace place : allPlaces) {
                        if (place.name.toLowerCase().contains(query) ||
                            place.campus.toLowerCase().contains(query) ||
                            place.addr.toLowerCase().contains(query)) {
                            filtered.add(place);
                        }
                    }
                }

                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredPlaces = (List<LocationSpinnerUtils.DalPlace>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}