package com.example.csci3130group1.ui.TutorialManagement;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.csci3130group1.R;

public class TutorialPreviewDialogFragment extends DialogFragment {
    
    private static final String ARG_TUTORIAL_NAME = "tutorial_name";
    private static final String ARG_TUTOR_NAME = "tutor_name";
    private static final String ARG_TOPIC = "topic";
    private static final String ARG_FEE = "fee";
    private static final String ARG_DATE = "date";
    private static final String ARG_START_TIME = "start_time";
    private static final String ARG_END_TIME = "end_time";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_LOCATION = "location";
    
    private PreviewConfirmListener listener;
    
    public interface PreviewConfirmListener {
        void onPreviewConfirmed();
    }
    
    public static TutorialPreviewDialogFragment newInstance(
            String tutorialName, String tutorName, String topic, String fee,
            String date, String startTime, String endTime, String description, String location) {
        
        TutorialPreviewDialogFragment fragment = new TutorialPreviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TUTORIAL_NAME, tutorialName);
        args.putString(ARG_TUTOR_NAME, tutorName);
        args.putString(ARG_TOPIC, topic);
        args.putString(ARG_FEE, fee);
        args.putString(ARG_DATE, date);
        args.putString(ARG_START_TIME, startTime);
        args.putString(ARG_END_TIME, endTime);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_LOCATION, location);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setPreviewConfirmListener(PreviewConfirmListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_tutorial_preview, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Get arguments
        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }
        
        // Find views
        TextView previewContent = view.findViewById(R.id.preview_content);
        TextView confirmButton = view.findViewById(R.id.confirm_button);
        TextView editButton = view.findViewById(R.id.edit_button);
        
        // Build preview content
        StringBuilder preview = new StringBuilder();
        preview.append("Tutorial Preview\n\n");
        preview.append("Tutorial: ").append(args.getString(ARG_TUTORIAL_NAME)).append("\n");
        preview.append("Tutor: ").append(args.getString(ARG_TUTOR_NAME)).append("\n");
        preview.append("Topic: ").append(args.getString(ARG_TOPIC)).append("\n");
        preview.append("Total Fee: $").append(args.getString(ARG_FEE)).append("\n");
        preview.append("Date: ").append(args.getString(ARG_DATE)).append("\n");
        preview.append("Time: ").append(args.getString(ARG_START_TIME)).append(" - ").append(args.getString(ARG_END_TIME)).append("\n");
        preview.append("Location: ").append(args.getString(ARG_LOCATION)).append("\n\n");
        preview.append("Description:\n").append(args.getString(ARG_DESCRIPTION));
        
        previewContent.setText(preview.toString());
        
        // Set up buttons
        confirmButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPreviewConfirmed();
            }
            dismiss();
        });
        
        editButton.setOnClickListener(v -> dismiss());
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Make dialog take up most of screen width
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}