package com.example.csci3130group1.utils;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.csci3130group1.R;
import com.example.csci3130group1.TutorialDetailsActivity;
import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TutorialSummaryHelper {

    public enum ViewMode {
        CURRENT_USER_PROFILE,
        OTHER_TUTOR_PROFILE
    }

    public static class TutorialSummaryConfig {
        private ViewMode viewMode;
        private String tutorId;
        private boolean showUpcomingSection;
        private boolean showStatsSection;
        private int maxUpcomingTutorials;

        public TutorialSummaryConfig(ViewMode viewMode) {
            this.viewMode = viewMode;
            this.showUpcomingSection = true;
            this.showStatsSection = true;
            this.maxUpcomingTutorials = -1; // -1 means show all
        }

        public TutorialSummaryConfig setTutorId(String tutorId) {
            this.tutorId = tutorId;
            return this;
        }

        public TutorialSummaryConfig setShowUpcomingSection(boolean show) {
            this.showUpcomingSection = show;
            return this;
        }

        public TutorialSummaryConfig setShowStatsSection(boolean show) {
            this.showStatsSection = show;
            return this;
        }

        public TutorialSummaryConfig setMaxUpcomingTutorials(int max) {
            this.maxUpcomingTutorials = max;
            return this;
        }

        public ViewMode getViewMode() { return viewMode; }
        public String getTutorId() { return tutorId; }
        public boolean shouldShowUpcomingSection() { return showUpcomingSection; }
        public boolean shouldShowStatsSection() { return showStatsSection; }
        public int getMaxUpcomingTutorials() { return maxUpcomingTutorials; }
    }

    public static void bindTutorialSummary(
            @NonNull Context context,
            @NonNull View rootView,
            @NonNull List<Tutorial> allTutorials,
            @NonNull TutorialSummaryConfig config) {

        List<Tutorial> upcomingTutorials = filterUpcomingTutorials(allTutorials);

        if (config.shouldShowStatsSection()) {
            bindTutorialStats(rootView, allTutorials, config);
        }

        if (config.shouldShowUpcomingSection()) {
            bindUpcomingTutorials(context, rootView, upcomingTutorials, config);
        }
    }

    private static void bindTutorialStats(View rootView, List<Tutorial> tutorials, TutorialSummaryConfig config) {
        if (tutorials.isEmpty()) {
            TextView tutorialStats = rootView.findViewById(R.id.tutorialStats);
            if (tutorialStats != null) {
                String emptyMessage = config.getViewMode() == ViewMode.CURRENT_USER_PROFILE 
                    ? "No tutorials created yet." 
                    : "No tutorials available.";
                tutorialStats.setText(emptyMessage);
            }
            return;
        }

        int totalTutorials = tutorials.size();
        int upcomingCount = 0;
        int pastCount = 0;

        for (Tutorial tutorial : tutorials) {
            if (isTutorialUpcoming(tutorial)) {
                upcomingCount++;
            } else {
                pastCount++;
            }
        }

        // Try to use modern stats row first - check both tutor and student versions
        View tutorStatsRow = rootView.findViewById(R.id.tutorStatsRow);
        TextView tutorTotalVal = rootView.findViewById(R.id.tutorTotalValue);
        TextView tutorUpcomingVal = rootView.findViewById(R.id.tutorUpcomingValue);
        TextView tutorCompletedVal = rootView.findViewById(R.id.tutorCompletedValue);
        
        View studentStatsRow = rootView.findViewById(R.id.studentStatsRow);
        TextView studentTotalVal = rootView.findViewById(R.id.studentTotalValue);
        TextView studentUpcomingVal = rootView.findViewById(R.id.studentUpcomingValue);
        TextView studentCompletedVal = rootView.findViewById(R.id.studentCompletedValue);
        
        TextView legacyTutorialStats = rootView.findViewById(R.id.tutorialStats);

        if (tutorStatsRow != null && tutorTotalVal != null && tutorUpcomingVal != null && tutorCompletedVal != null) {
            // Use modern tutor stats row layout
            tutorStatsRow.setVisibility(View.VISIBLE);
            if (legacyTutorialStats != null) legacyTutorialStats.setVisibility(View.GONE);
            
            tutorTotalVal.setText(String.valueOf(totalTutorials));
            tutorUpcomingVal.setText(String.valueOf(upcomingCount));
            tutorCompletedVal.setText(String.valueOf(pastCount));
        } else if (studentStatsRow != null && studentTotalVal != null && studentUpcomingVal != null && studentCompletedVal != null) {
            // Use modern student stats row layout
            studentStatsRow.setVisibility(View.VISIBLE);
            if (legacyTutorialStats != null) legacyTutorialStats.setVisibility(View.GONE);
            
            studentTotalVal.setText(String.valueOf(totalTutorials));
            studentUpcomingVal.setText(String.valueOf(upcomingCount));
            studentCompletedVal.setText(String.valueOf(pastCount));
        } else if (legacyTutorialStats != null) {
            // Fall back to legacy text format
            String statsText;
            if (config.getViewMode() == ViewMode.CURRENT_USER_PROFILE) {
                statsText = String.format(Locale.getDefault(),
                    "Total Tutorials: %d\nUpcoming: %d\nCompleted: %d",
                    totalTutorials, upcomingCount, pastCount);
            } else {
                statsText = String.format(Locale.getDefault(),
                    "Total Tutorials: %d\nUpcoming: %d\nPast: %d",
                    totalTutorials, upcomingCount, pastCount);
            }
            legacyTutorialStats.setText(statsText);
        }
    }

    private static void bindUpcomingTutorials(Context context, View rootView, 
            List<Tutorial> upcomingTutorials, TutorialSummaryConfig config) {
        
        View upcomingCard = rootView.findViewById(R.id.upcoming_tutorials_card);
        LinearLayout upcomingList = rootView.findViewById(R.id.upcomingTutorialsList);
        
        if (upcomingCard == null || upcomingList == null) return;

        if (upcomingTutorials.isEmpty()) {
            upcomingCard.setVisibility(View.GONE);
            return;
        }

        upcomingCard.setVisibility(View.VISIBLE);
        upcomingList.removeAllViews();

        int maxToShow = config.getMaxUpcomingTutorials();
        int tutorialsToShow = maxToShow > 0 ? Math.min(upcomingTutorials.size(), maxToShow) : upcomingTutorials.size();

        for (int i = 0; i < tutorialsToShow; i++) {
            Tutorial tutorial = upcomingTutorials.get(i);
            View tutorialCardView = LayoutInflater.from(context)
                .inflate(R.layout.tutorial_card_item, upcomingList, false);
            
            bindTutorialCard(context, tutorialCardView, tutorial, config);
            upcomingList.addView(tutorialCardView);
        }
    }

    public static void bindTutorialCard(Context context, View tutorialCardView, 
            Tutorial tutorial, TutorialSummaryConfig config) {
        
        TextView tutorialName = tutorialCardView.findViewById(R.id.tutorialCardName);
        TextView tutorialTutor = tutorialCardView.findViewById(R.id.tutorialCardTutor);
        TextView tutorialDateTime = tutorialCardView.findViewById(R.id.tutorialCardDateTime);
        TextView tutorialLocation = tutorialCardView.findViewById(R.id.tutorialCardLocation);

        tutorialName.setText(tutorial.getTutorialName() != null ? tutorial.getTutorialName() : "Unnamed Tutorial");
        tutorialTutor.setText(tutorial.getTutorName() != null ? tutorial.getTutorName() : "Unknown Tutor");

        String dateTime = String.format(Locale.getDefault(), "%s at %s - %s",
                tutorial.getDate() != null ? tutorial.getDate() : "No date",
                tutorial.getStartTime() != null ? tutorial.getStartTime() : "TBD",
                tutorial.getEndTime() != null ? tutorial.getEndTime() : "TBD");
        tutorialDateTime.setText(dateTime);

        tutorialLocation.setText(tutorial.getAddress() != null ? tutorial.getAddress() : "Location TBD");

        tutorialCardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TutorialDetailsActivity.class);
            intent.putExtra("tutorialId", tutorial.getTutorialId());
            intent.putExtra("tutorialName", tutorial.getTutorialName());
            intent.putExtra("tutorName", tutorial.getTutorName());
            intent.putExtra("fee", tutorial.getFee());
            intent.putExtra("date", tutorial.getDate());
            intent.putExtra("startTime", tutorial.getStartTime());
            intent.putExtra("endTime", tutorial.getEndTime());
            intent.putExtra("address", tutorial.getAddress());
            
            if (config.getViewMode() == ViewMode.CURRENT_USER_PROFILE) {
                intent.putExtra("isAlreadyRegistered", true);
            }
            
            context.startActivity(intent);
        });
    }

    private static List<Tutorial> filterUpcomingTutorials(List<Tutorial> allTutorials) {
        List<Tutorial> upcomingTutorials = new java.util.ArrayList<>();
        for (Tutorial tutorial : allTutorials) {
            if (isTutorialUpcoming(tutorial)) {
                upcomingTutorials.add(tutorial);
            }
        }
        return upcomingTutorials;
    }

    public static boolean isTutorialUpcoming(Tutorial tutorial) {
        // Prefer robust numeric check if available
        try {
            Long endTs = tutorial.getEndTimestamp();
            if (endTs != null) {
                return endTs > System.currentTimeMillis();
            }
        } catch (Exception ignored) {}

        if (tutorial.getDate() == null) return false;
        
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date tutorialDate = dateFormat.parse(tutorial.getDate());
            Date currentDate = new Date();
            
            // Consider tutorials on the same date as upcoming if time not known; be lenient
            if (tutorialDate == null) return false;
            // Normalize to start of day for comparison
            java.util.Calendar calA = java.util.Calendar.getInstance(); calA.setTime(tutorialDate);
            calA.set(java.util.Calendar.HOUR_OF_DAY, 23);
            calA.set(java.util.Calendar.MINUTE, 59);
            calA.set(java.util.Calendar.SECOND, 59);
            calA.set(java.util.Calendar.MILLISECOND, 999);
            return calA.getTimeInMillis() > currentDate.getTime();
        } catch (ParseException e) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date tutorialDate = dateFormat.parse(tutorial.getDate());
                Date currentDate = new Date();
                
                if (tutorialDate == null) return false;
                java.util.Calendar calA = java.util.Calendar.getInstance(); calA.setTime(tutorialDate);
                calA.set(java.util.Calendar.HOUR_OF_DAY, 23);
                calA.set(java.util.Calendar.MINUTE, 59);
                calA.set(java.util.Calendar.SECOND, 59);
                calA.set(java.util.Calendar.MILLISECOND, 999);
                return calA.getTimeInMillis() > currentDate.getTime();
            } catch (ParseException e2) {
                return false;
            }
        }
    }

    public static void updateSectionHeader(View rootView, int resourceId, String text) {
        TextView header = rootView.findViewById(resourceId);
        if (header != null) {
            header.setText(text);
        }
    }

    public static void setSectionVisibility(View rootView, int resourceId, boolean visible) {
        View section = rootView.findViewById(resourceId);
        if (section != null) {
            section.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
