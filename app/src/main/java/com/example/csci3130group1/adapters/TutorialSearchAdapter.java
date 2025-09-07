package com.example.csci3130group1.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.R;
import com.example.csci3130group1.TutorialDetailsActivity;
import com.example.csci3130group1.models.TutorialSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TutorialSearchAdapter extends RecyclerView.Adapter<TutorialSearchAdapter.TutorialViewHolder> {

    private Context context;
    private List<TutorialSession> tutorials;

    public TutorialSearchAdapter(Context context) {
        this.context = context;
        this.tutorials = new ArrayList<>();
    }

    @NonNull
    @Override
    public TutorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tutorial_search, parent, false);
        return new TutorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorialViewHolder holder, int position) {
        TutorialSession tutorial = tutorials.get(position);
        
        // Set tutorial information
        holder.tutorialName.setText(tutorial.getTutorialName() != null ? 
            tutorial.getTutorialName() : "Unnamed Tutorial");
        
        holder.tutorialTopic.setText(tutorial.getTopic() != null ? 
            tutorial.getTopic() : "General");
        
        // Format fee display
        String feeText = "Free";
        if (tutorial.getFee() != null && !tutorial.getFee().equals("0")) {
            feeText = "$" + tutorial.getFee();
        }
        holder.tutorialFee.setText(feeText);
        
        holder.tutorName.setText(tutorial.getTutorName() != null ? 
            tutorial.getTutorName() : "Unknown Tutor");
        
        // Format date and time
        String dateTime = String.format(Locale.getDefault(), "%s at %s - %s",
                tutorial.getDate() != null ? tutorial.getDate() : "No date",
                tutorial.getStartTime() != null ? tutorial.getStartTime() : "TBD",
                tutorial.getEndTime() != null ? tutorial.getEndTime() : "TBD");
        holder.tutorialDateTime.setText(dateTime);
        
        holder.tutorialLocation.setText(tutorial.getAddress() != null ? 
            tutorial.getAddress() : "Location TBD");
        
        holder.tutorialDescription.setText(tutorial.getDescription() != null ? 
            tutorial.getDescription() : "No description available");
        
        // Set click listeners
        holder.viewDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, TutorialDetailsActivity.class);
            intent.putExtra("tutorialId", tutorial.getTutorialId());
            intent.putExtra("tutorialName", tutorial.getTutorialName());
            intent.putExtra("tutorName", tutorial.getTutorName());
            intent.putExtra("fee", tutorial.getFee());
            intent.putExtra("date", tutorial.getDate());
            intent.putExtra("startTime", tutorial.getStartTime());
            intent.putExtra("endTime", tutorial.getEndTime());
            intent.putExtra("address", tutorial.getAddress());
            intent.putExtra("description", tutorial.getDescription());
            context.startActivity(intent);
        });
        
        // Make entire card clickable
        holder.itemView.setOnClickListener(v -> holder.viewDetailsButton.performClick());
    }

    @Override
    public int getItemCount() {
        return tutorials.size();
    }

    public void updateTutorials(List<TutorialSession> newTutorials) {
        this.tutorials.clear();
        if (newTutorials != null) {
            this.tutorials.addAll(newTutorials);
        }
        notifyDataSetChanged();
    }

    public void clearTutorials() {
        this.tutorials.clear();
        notifyDataSetChanged();
    }

    static class TutorialViewHolder extends RecyclerView.ViewHolder {
        TextView tutorialName, tutorialTopic, tutorialFee, tutorName;
        TextView tutorialDateTime, tutorialLocation, tutorialDescription;
        Button viewDetailsButton;

        public TutorialViewHolder(@NonNull View itemView) {
            super(itemView);
            tutorialName = itemView.findViewById(R.id.tutorialName);
            tutorialTopic = itemView.findViewById(R.id.tutorialTopic);
            tutorialFee = itemView.findViewById(R.id.tutorialFee);
            tutorName = itemView.findViewById(R.id.tutorName);
            tutorialDateTime = itemView.findViewById(R.id.tutorialDateTime);
            tutorialLocation = itemView.findViewById(R.id.tutorialLocation);
            tutorialDescription = itemView.findViewById(R.id.tutorialDescription);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
        }
    }
}