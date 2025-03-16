package com.example.csci3130group1.ui.search_for_tutorials;

import android.content.Context;
import android.content.Intent;
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
    // Backup copy of the full tutorials list
    private List<Tutorial> originalTutorials;

    public TutorialAdapter(Context context, List<Tutorial> tutorials) {
        this.context = context;
        this.tutorials = tutorials;
        // Create a backup copy for filtering purposes
        this.originalTutorials = new ArrayList<>(tutorials);
    }

    // Call this method when new data is loaded (e.g., from Firebase)
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
        title.setText(tutorial.getTopic());
        String detailText = "Tutor: " + tutorial.getName() + " (" + tutorial.getDegree() + ")\n" +
                "Location: " + tutorial.getCity() + ", " + tutorial.getProvince() + ", " + tutorial.getCountry() + "\n" +
                "Fee: $" + tutorial.getFee() + " | Duration: " + tutorial.getDuration() + " mins";
        details.setText(detailText);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent detailIntent = new Intent(context, TutorialDetailsActivity.class);
                detailIntent.putExtra("topic", tutorial.getTopic());
                detailIntent.putExtra("fee", tutorial.getFee());
                detailIntent.putExtra("duration", tutorial.getDuration());
                detailIntent.putExtra("description", tutorial.getDescription());
                detailIntent.putExtra("city", tutorial.getCity());
                detailIntent.putExtra("province", tutorial.getProvince());
                detailIntent.putExtra("country", tutorial.getCountry());
                detailIntent.putExtra("name", tutorial.getName());
                detailIntent.putExtra("degree", tutorial.getDegree());
                context.startActivity(detailIntent);
            }
        });

        Log.d(TAG, "getView for tutorial: " + tutorial.getTopic());
        return convertView;
    }

    /**
     * Filters the tutorials based on location, fee, and duration.
     * For duration, only tutorials with durations strictly less than the entered max will be displayed.
     *
     * @param locationFilter the text for location filter (case-insensitive, partial match, applied to city)
     * @param feeFilter the maximum fee allowed (as a string to be parsed as a number)
     * @param durationFilter the maximum duration allowed (as a string to be parsed as a number)
     */
    public void filter(String locationFilter, String feeFilter, String durationFilter) {
        Log.d(TAG, "Starting filter with " + originalTutorials.size() + " original tutorials.");
        List<Tutorial> filteredList = new ArrayList<>();

        for (Tutorial tutorial : originalTutorials) {
            boolean matches = true;

            Log.d(TAG, "Checking tutorial: " + tutorial.getTopic() +
                    " | City: " + tutorial.getCity() +
                    " | Fee: " + tutorial.getFee() +
                    " | Duration: " + tutorial.getDuration());

            if (locationFilter != null && !locationFilter.isEmpty()) {
                String tutorialCity = tutorial.getCity();
                if (tutorialCity == null) {
                    Log.d(TAG, "Tutorial " + tutorial.getTopic() + " city is null.");
                    matches = false;
                } else if (!tutorialCity.toLowerCase().contains(locationFilter.toLowerCase())) {
                    Log.d(TAG, "Tutorial " + tutorial.getTopic() + " city (" + tutorialCity + ") does not contain filter " + locationFilter);
                    matches = false;
                }
            }

            // Check fee filter
            if (matches && feeFilter != null && !feeFilter.isEmpty()) {
                try {
                    double maxFee = Double.parseDouble(feeFilter);
                    double tutorialFee = Double.parseDouble(tutorial.getFee());
                    if (tutorialFee > maxFee) {
                        Log.d(TAG, "Tutorial " + tutorial.getTopic() + " fee (" + tutorialFee + ") is greater than filter max fee " + maxFee);
                        matches = false;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Fee parsing error for tutorial: " + tutorial.getTopic(), e);
                    matches = false;
                }
            }

            // Check duration filter: Only include tutorials with durations strictly less than the max.
            if (matches && durationFilter != null && !durationFilter.isEmpty()) {
                try {
                    double maxDuration = Double.parseDouble(durationFilter);
                    double tutorialDuration = Double.parseDouble(tutorial.getDuration());
                    if (tutorialDuration >= maxDuration) { // Exclude if duration is equal or greater.
                        Log.d(TAG, "Tutorial " + tutorial.getTopic() + " duration (" + tutorialDuration + ") is not less than filter max duration " + maxDuration);
                        matches = false;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Duration parsing error for tutorial: " + tutorial.getTopic(), e);
                    matches = false;
                }
            }

            if (matches) {
                filteredList.add(tutorial);
                Log.d(TAG, "Tutorial matches filter: " + tutorial.getTopic());
            }
        }

        Log.d(TAG, "Filtered list size: " + filteredList.size());
        // Update the list and notify that the dataset has changed.
        tutorials = filteredList;
        notifyDataSetChanged();
    }
}
