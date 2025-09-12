package com.example.csci3130group1.ui.community;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csci3130group1.R;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Fullscreen-style dialog with an explicit dimmed backdrop view.
 * Ensures only the background is dimmed, not the modal content itself.
 */
public class CreateThreadDialogFragment extends DialogFragment {

    private CommunityViewModel communityViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Dialog_Fullscreen_Transparent);
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
        return inflater.inflate(R.layout.dialog_create_thread_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Share ViewModel with the parent CommunityFragment
        communityViewModel = new ViewModelProvider(requireParentFragment())
                .get(CommunityViewModel.class);

        // Prevent dismissal when tapping the dimmed backdrop
        View backdrop = view.findViewById(R.id.backdrop);
        if (backdrop != null) {
            backdrop.setOnClickListener(v -> {
                // Do nothing - prevents dismissal on backdrop click
            });
        }

        // Setup category dropdown
        android.widget.AutoCompleteTextView categorySpinner = view.findViewById(R.id.spinner_thread_category);
        if (categorySpinner != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    communityViewModel.getThreadCategories());
            categorySpinner.setAdapter(adapter);
            categorySpinner.setText(communityViewModel.getThreadCategories()[0], false);
        }

        // Buttons
        View btnCancel = view.findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        View btnCreate = view.findViewById(R.id.btn_create);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                TextInputEditText titleEt = view.findViewById(R.id.edit_thread_title);
                TextInputEditText descEt = view.findViewById(R.id.edit_thread_description);
                android.widget.AutoCompleteTextView catSpin = view.findViewById(R.id.spinner_thread_category);

                String title = titleEt != null && titleEt.getText() != null ? titleEt.getText().toString() : "";
                String description = descEt != null && descEt.getText() != null ? descEt.getText().toString() : "";
                String category = catSpin != null ? catSpin.getText().toString() : "";

                communityViewModel.createThread(title, description, category);
                dismiss();
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make container expand to full screen so backdrop covers entire activity
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
}

