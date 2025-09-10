package com.example.csci3130group1.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.csci3130group1.R;
import com.example.csci3130group1.TutorProfileActivity;
import com.example.csci3130group1.models.TutorProfile;

import java.util.ArrayList;
import java.util.List;

public class TutorSearchAdapter extends RecyclerView.Adapter<TutorSearchAdapter.TutorViewHolder> {

    private Context context;
    private List<TutorProfile> tutors;

    public TutorSearchAdapter(Context context) {
        this.context = context;
        this.tutors = new ArrayList<>();
    }

    @NonNull
    @Override
    public TutorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tutor_search, parent, false);
        return new TutorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorViewHolder holder, int position) {
        TutorProfile tutor = tutors.get(position);
        
        // Set tutor information
        holder.tutorName.setText(tutor.getName() != null ? tutor.getName() : "Unknown Tutor");
        
        holder.tutorDegree.setText(tutor.getDegree() != null ? tutor.getDegree() : "");
        
        // Set tutorial count
        holder.tutorialCount.setText(tutor.getTutorialCountText());
        
        // Set rating
        if (tutor.getReviewCount() > 0) {
            holder.tutorRating.setText(tutor.getFormattedRating());
            holder.ratingContainer.setVisibility(View.VISIBLE);
        } else {
            holder.ratingContainer.setVisibility(View.GONE);
        }
        
        // Set description
        if (tutor.hasDescription()) {
            holder.tutorDescription.setText(tutor.getDescription());
            holder.tutorDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tutorDescription.setVisibility(View.GONE);
        }
        
        // Load profile picture
        if (tutor.hasProfilePicture()) {
            Glide.with(context)
                .load(tutor.getProfilePictureUrl())
                .circleCrop()
                .placeholder(R.drawable.circle_background)
                .error(R.drawable.circle_background)
                .into(holder.tutorProfilePicture);
        } else {
            holder.tutorProfilePicture.setImageResource(R.drawable.circle_background);
        }
        
        View.OnClickListener openProfile = v -> {
            Intent intent = new Intent(context, TutorProfileActivity.class);
            intent.putExtra("tutorId", tutor.getTutorId());
            context.startActivity(intent);
        };
        holder.itemView.setOnClickListener(openProfile);
        if (holder.viewProfileButton != null) {
            holder.viewProfileButton.setOnClickListener(openProfile);
            holder.viewProfileButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return tutors.size();
    }

    public void updateTutors(List<TutorProfile> newTutors) {
        this.tutors.clear();
        if (newTutors != null) {
            this.tutors.addAll(newTutors);
        }
        notifyDataSetChanged();
    }

    public void clearTutors() {
        this.tutors.clear();
        notifyDataSetChanged();
    }

    static class TutorViewHolder extends RecyclerView.ViewHolder {
        ImageView tutorProfilePicture;
        TextView tutorName, tutorDegree, tutorialCount, tutorRating, tutorDescription;
        Button viewProfileButton;
        LinearLayout ratingContainer;

        public TutorViewHolder(@NonNull View itemView) {
            super(itemView);
            tutorProfilePicture = itemView.findViewById(R.id.tutorProfilePicture);
            tutorName = itemView.findViewById(R.id.tutorName);
            tutorDegree = itemView.findViewById(R.id.tutorDegree);
            tutorialCount = itemView.findViewById(R.id.tutorialCount);
            tutorRating = itemView.findViewById(R.id.tutorRating);
            tutorDescription = itemView.findViewById(R.id.tutorDescription);
            viewProfileButton = itemView.findViewById(R.id.viewProfileButton);
            ratingContainer = itemView.findViewById(R.id.ratingContainer);
        }
    }
}
