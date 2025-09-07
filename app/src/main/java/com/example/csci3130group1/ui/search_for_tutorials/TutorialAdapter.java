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
        TextView tutorInfo = convertView.findViewById(R.id.tutorInfo);
        TextView details = convertView.findViewById(R.id.tutorialDetails);

        Tutorial tutorial = tutorials.get(position);
        
        // Set tutorial title
        title.setText(tutorial.getTopic() != null ? tutorial.getTopic() : tutorial.getTutorialName());
        
        // Set tutor information
        String tutorText = tutorial.getTutorName();
        if (tutorial.getTutorDegree() != null && !tutorial.getTutorDegree().isEmpty()) {
            tutorText += " (" + tutorial.getTutorDegree() + ")";
        }
        tutorInfo.setText(tutorText != null ? tutorText : "Unknown Tutor");
        
        // Set time and fee details
        String timeText = "";
        if (tutorial.getStartTime() != null && tutorial.getEndTime() != null) {
            timeText = tutorial.getStartTime() + " - " + tutorial.getEndTime();
        }
        String feeText = tutorial.getFee() != null ? "$" + tutorial.getFee() : "Free";
        String detailText = timeText + (timeText.isEmpty() ? "" : " | ") + feeText;
        details.setText(detailText);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to TutorialDetailsActivity with the tutorial ID
                if (tutorial.getTutorialId() != null) {
                    Intent detailIntent = new Intent(context, TutorialDetailsActivity.class);
                    detailIntent.putExtra("tutorialId", tutorial.getTutorialId());
                    context.startActivity(detailIntent);
                } else {
                    // Fallback to old method if tutorialId is not available
                    Intent detailIntent = new Intent(context, TutorialDetailsActivity.class);
                    detailIntent.putExtra("topic", tutorial.getTopic());
                    detailIntent.putExtra("fee", tutorial.getFee());
                    detailIntent.putExtra("duration", tutorial.getStartTime() + " - " + tutorial.getEndTime());
                    detailIntent.putExtra("description", tutorial.getDescription());
                    detailIntent.putExtra("streetAddress", tutorial.getStreetAddress());
                    detailIntent.putExtra("postalCode", tutorial.getPostalCode());
                    detailIntent.putExtra("name", tutorial.getTutorName());
                    detailIntent.putExtra("degree", tutorial.getTutorDegree());
                    detailIntent.putExtra("tutorUserId", tutorial.getTutorId());
                    context.startActivity(detailIntent);
                }
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
                    " | Location: " + tutorial.getAddress() +
                    " | Fee: " + tutorial.getFee() +
                    " | Time: " + tutorial.getStartTime() + "-" + tutorial.getEndTime());

            if (locationFilter != null && !locationFilter.isEmpty()) {
                String tutorialLocation = tutorial.getAddress();
                if (tutorialLocation == null) {
                    Log.d(TAG, "Tutorial " + tutorial.getTopic() + " location is null.");
                    matches = false;
                } else if (!tutorialLocation.toLowerCase().contains(locationFilter.toLowerCase())) {
                    Log.d(TAG, "Tutorial " + tutorial.getTopic() + " location (" + tutorialLocation + ") does not contain filter " + locationFilter);
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

            // Duration filter disabled - we now use separate start/end times
            // TODO: Implement time-based filtering using startTime and endTime
            if (matches && durationFilter != null && !durationFilter.isEmpty()) {
                Log.d(TAG, "Duration filter temporarily disabled - using start/end times instead");
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
