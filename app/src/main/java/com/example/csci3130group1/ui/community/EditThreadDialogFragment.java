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

public class EditThreadDialogFragment extends DialogFragment {

    private static final String ARG_THREAD_ID = "arg_thread_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESC = "arg_desc";
    private static final String ARG_CATEGORY = "arg_category";

    private CommunityViewModel communityViewModel;

    public static EditThreadDialogFragment newInstance(String threadId, String title, String description, String category) {
        EditThreadDialogFragment f = new EditThreadDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_THREAD_ID, threadId);
        b.putString(ARG_TITLE, title);
        b.putString(ARG_DESC, description);
        b.putString(ARG_CATEGORY, category);
        f.setArguments(b);
        return f;
    }

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
        return inflater.inflate(R.layout.dialog_edit_thread_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        communityViewModel = new ViewModelProvider(requireActivity()).get(CommunityViewModel.class);

        View backdrop = view.findViewById(R.id.backdrop);
        if (backdrop != null) backdrop.setOnClickListener(v -> dismiss());

        String threadId = getArguments() != null ? getArguments().getString(ARG_THREAD_ID) : null;
        String title = getArguments() != null ? getArguments().getString(ARG_TITLE) : "";
        String desc = getArguments() != null ? getArguments().getString(ARG_DESC) : "";
        String category = getArguments() != null ? getArguments().getString(ARG_CATEGORY) : "";

        TextInputEditText titleEt = view.findViewById(R.id.edit_thread_title);
        TextInputEditText descEt = view.findViewById(R.id.edit_thread_description);
        android.widget.AutoCompleteTextView catSpin = view.findViewById(R.id.spinner_thread_category);

        if (titleEt != null) titleEt.setText(title);
        if (descEt != null) descEt.setText(desc);

        if (catSpin != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    communityViewModel.getThreadCategories());
            catSpin.setAdapter(adapter);
            if (category != null && !category.isEmpty()) {
                catSpin.setText(category, false);
            } else {
                catSpin.setText(communityViewModel.getThreadCategories()[0], false);
            }
        }

        View btnCancel = view.findViewById(R.id.btn_cancel);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());

        View btnSave = view.findViewById(R.id.btn_save);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String newTitle = titleEt != null && titleEt.getText() != null ? titleEt.getText().toString() : "";
                String newDesc = descEt != null && descEt.getText() != null ? descEt.getText().toString() : "";
                String newCat = catSpin != null ? catSpin.getText().toString() : "";

                communityViewModel.editThread(threadId, newTitle, newDesc, newCat);
                dismiss();
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
}

