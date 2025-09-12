package com.example.csci3130group1.ui.community;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csci3130group1.R;
import com.google.android.material.textfield.TextInputEditText;

public class EditReplyDialogFragment extends DialogFragment {

    private static final String ARG_REPLY_ID = "arg_reply_id";
    private static final String ARG_CONTENT = "arg_content";

    private CommunityViewModel communityViewModel;

    public static EditReplyDialogFragment newInstance(String replyId, String content) {
        EditReplyDialogFragment f = new EditReplyDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_REPLY_ID, replyId);
        b.putString(ARG_CONTENT, content);
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
        return inflater.inflate(R.layout.dialog_edit_reply_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        communityViewModel = new ViewModelProvider(requireActivity()).get(CommunityViewModel.class);

        View backdrop = view.findViewById(R.id.backdrop);
        if (backdrop != null) backdrop.setOnClickListener(v -> dismiss());

        String replyId = getArguments() != null ? getArguments().getString(ARG_REPLY_ID) : null;
        String content = getArguments() != null ? getArguments().getString(ARG_CONTENT) : "";

        TextInputEditText contentEt = view.findViewById(R.id.edit_reply_content);
        if (contentEt != null) contentEt.setText(content);

        View btnCancel = view.findViewById(R.id.btn_cancel);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());

        View btnSave = view.findViewById(R.id.btn_save);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String newContent = contentEt != null && contentEt.getText() != null ? contentEt.getText().toString() : "";
                communityViewModel.editReply(replyId, newContent);
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

