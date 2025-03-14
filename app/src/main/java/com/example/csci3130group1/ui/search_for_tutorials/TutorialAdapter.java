package com.example.csci3130group1.ui.search_for_tutorials;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.csci3130group1.R;

import java.util.ArrayList;
import java.util.List;

public class TutorialAdapter extends BaseAdapter {

    private static final String TAG = "TutorialAdapter";
    private Context context;
    // Current list (may be filtered)
    private List<Tutorial> tutorials;
    // Backup copy of the original tutorials list
    private List<Tutorial> originalTutorials;

    public TutorialAdapter(Context context, List<Tutorial> tutorials) {
        this.context = context;
        this.tutorials = tutorials;
        // Create a backup copy for filtering purposes
        this.originalTutorials = new ArrayList<>(tutorials);
    }

    // Method to update the adapter's data and backup list
    public void updateTutorials(List<Tutorial> newTutorials) {
        this.tutorials = newTutorials;
        this.originalTutorials = new ArrayList<>(newTutorials);
        Log.d(TAG, "Adapter updated with " + newTutorials.size() + " tutorials.");
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tutorials.size();
    }

    @Override
    public Object getItem(int position) {
        return tutorials.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_tutorial, parent, false);
        }

        TextView title = convertView.findViewById(R.id.tutorialTitle);
        TextView details = convertView.findViewById(R.id.tutorialDetails);
        ImageView mapIcon = convertView.findViewById(R.id.mapIcon);

        Tutorial tutorial = tutorials.get(position);
        title.setText(tutorial.getTitle());
        details.setText("Location: " + tutorial.getLocation()
                + " | Fee: $" + tutorial.getFee()
                + " | " + tutorial.getDuration() + " hrs");

        Log.d(TAG, "getView for tutorial: " + tutorial.getTitle());
        return convertView;
    }

    /**
     * Filters the tutorials based on location, fee, and duration.
     * For duration, only tutorials with durations strictly less than the entered max will be displayed.
     *
     * @param locationFilter the text for location filter (case-insensitive, partial match)
     * @param feeFilter the maximum fee allowed (as a string to be parsed as a number)
     * @param durationFilter the maximum duration allowed (as a string to be parsed as a number)
     */
    public void filter(String locationFilter, String feeFilter, String durationFilter) {
        Log.d(TAG, "Starting filter with " + originalTutorials.size() + " original tutorials.");
        List<Tutorial> filteredList = new ArrayList<>();

        for (Tutorial tutorial : originalTutorials) {
            boolean matches = true;

            // Check location filter (case-insensitive partial match)
            if (locationFilter != null && !locationFilter.isEmpty()) {
                if (!tutorial.getLocation().toLowerCase().contains(locationFilter.toLowerCase())) {
                    matches = false;
                }
            }

            // Check fee filter
            if (feeFilter != null && !feeFilter.isEmpty()) {
                try {
                    double maxFee = Double.parseDouble(feeFilter);
                    double tutorialFee = Double.parseDouble(tutorial.getFee());
                    if (tutorialFee > maxFee) {
                        matches = false;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Fee parsing error for tutorial: " + tutorial.getTitle());
                    matches = false;
                }
            }

            // Check duration filter: Only include tutorials with durations strictly less than the max.
            if (durationFilter != null && !durationFilter.isEmpty()) {
                try {
                    double maxDuration = Double.parseDouble(durationFilter);
                    double tutorialDuration = Double.parseDouble(tutorial.getDuration());
                    // Use > to filter out tutorials with duration equal to or greater than maxDuration.
                    if (tutorialDuration > maxDuration) {
                        matches = false;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Duration parsing error for tutorial: " + tutorial.getTitle());
                    matches = false;
                }
            }

            if (matches) {
                filteredList.add(tutorial);
                Log.d(TAG, "Tutorial matches filter: " + tutorial.getTitle());
            }
        }

        Log.d(TAG, "Filtered list size: " + filteredList.size());
        // Update the current list and refresh the ListView
        tutorials = filteredList;
        notifyDataSetChanged();
    }
}
