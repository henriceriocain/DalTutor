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
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TutorialSearchAdapter extends RecyclerView.Adapter<TutorialSearchAdapter.TutorialViewHolder> {

    private Context context;
    private List<TutorialSession> tutorials;
    private String currentUserId;

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

        // Capacity badge: show registered/limit if limited; hide if unlimited
        TextView capBadge = holder.capacityBadge;
        Integer cap = tutorial.getCapacity();
        int registeredCount = tutorial.getRegisteredStudentCount();
        if (cap != null && cap > 0) {
            capBadge.setVisibility(View.VISIBLE);
            capBadge.setText(String.format(Locale.getDefault(), "%d/%d", registeredCount, cap));
            long remaining = Math.max(cap - registeredCount, 0);
            capBadge.setTextColor(getCapacityColor(remaining, cap));
        } else {
            capBadge.setVisibility(View.GONE);
        }
        
        // Card click navigates to details
        View.OnClickListener openDetails = v -> {
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
            boolean isRegistered = false;
            if (currentUserId != null) {
                isRegistered = tutorial.isStudentRegistered(currentUserId);
            }
            intent.putExtra("isAlreadyRegistered", isRegistered);
            context.startActivity(intent);
        };
        holder.itemView.setOnClickListener(openDetails);
        if (holder.viewDetailsButton != null) {
            holder.viewDetailsButton.setOnClickListener(openDetails);
            holder.viewDetailsButton.setVisibility(View.GONE);
        }
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

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    public void clearTutorials() {
        this.tutorials.clear();
        notifyDataSetChanged();
    }

    static class TutorialViewHolder extends RecyclerView.ViewHolder {
        TextView tutorialName, tutorialTopic, tutorialFee, tutorName;
        TextView tutorialDateTime, tutorialLocation, tutorialDescription;
        Button viewDetailsButton;
        TextView capacityBadge;

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
            capacityBadge = itemView.findViewById(R.id.capacityBadge);
        }
    }

    // Map remaining/capacity to a color ranging from green (empty) to red (full)
    private int getCapacityColor(long remaining, long capacity) {
        if (capacity <= 0) return Color.parseColor("#6B7280"); // neutral
        float ratio = Math.max(0f, Math.min(1f, remaining / (float) capacity));
        int green = Color.parseColor("#059669"); // emerald-600
        int yellow = Color.parseColor("#F59E0B"); // amber-500
        int red = Color.parseColor("#DC2626"); // red-600

        if (ratio >= 0.5f) {
            float t = (ratio - 0.5f) / 0.5f; // 0..1 from yellow->green
            return lerpColor(yellow, green, t);
        } else {
            float t = ratio / 0.5f; // 0..1 from red->yellow
            return lerpColor(red, yellow, t);
        }
    }

    private int lerpColor(int startColor, int endColor, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (Color.alpha(startColor) + (Color.alpha(endColor) - Color.alpha(startColor)) * t);
        int r = (int) (Color.red(startColor) + (Color.red(endColor) - Color.red(startColor)) * t);
        int g = (int) (Color.green(startColor) + (Color.green(endColor) - Color.green(startColor)) * t);
        int b = (int) (Color.blue(startColor) + (Color.blue(endColor) - Color.blue(startColor)) * t);
        return Color.argb(a, r, g, b);
    }
}
