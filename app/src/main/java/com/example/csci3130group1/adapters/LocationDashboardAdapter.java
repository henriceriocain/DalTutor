package com.example.csci3130group1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.csci3130group1.R;
import java.util.List;

public class LocationDashboardAdapter extends RecyclerView.Adapter<LocationDashboardAdapter.LocationViewHolder> {

    public static class LocationInfo {
        public final String locationName;
        public final String placeId;
        public final int tutorialCount;

        public LocationInfo(String locationName, String placeId, int tutorialCount) {
            this.locationName = locationName;
            this.placeId = placeId;
            this.tutorialCount = tutorialCount;
        }
    }

    public interface OnLocationClickListener {
        void onLocationClick(LocationInfo location);
    }

    private List<LocationInfo> locations;
    private OnLocationClickListener clickListener;

    public LocationDashboardAdapter(List<LocationInfo> locations, OnLocationClickListener clickListener) {
        this.locations = locations;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_dashboard, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        LocationInfo location = locations.get(position);
        holder.bind(location);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public void updateLocations(List<LocationInfo> newLocations) {
        this.locations = newLocations;
        notifyDataSetChanged();
    }

    class LocationViewHolder extends RecyclerView.ViewHolder {
        private TextView textLocationName;
        private TextView textTutorialCount;
        private TextView textCountBadge;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            textLocationName = itemView.findViewById(R.id.textLocationName);
            textTutorialCount = itemView.findViewById(R.id.textTutorialCount);
            textCountBadge = itemView.findViewById(R.id.textCountBadge);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onLocationClick(locations.get(position));
                }
            });
        }

        public void bind(LocationInfo location) {
            textLocationName.setText(location.locationName);
            
            String countText = location.tutorialCount == 1 
                ? "1 tutorial available" 
                : location.tutorialCount + " tutorials available";
            textTutorialCount.setText(countText);
            
            textCountBadge.setText(String.valueOf(location.tutorialCount));
        }
    }
}