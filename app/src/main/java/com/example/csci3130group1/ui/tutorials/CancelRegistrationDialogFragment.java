package com.example.csci3130group1.ui.tutorials;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.csci3130group1.R;

public class CancelRegistrationDialogFragment extends DialogFragment {

    public interface OnConfirmListener { void onConfirmCancel(); }
    private OnConfirmListener listener;

    public void setOnConfirmListener(OnConfirmListener l) { this.listener = l; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Dialog_Fullscreen_Transparent);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_cancel_registration_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View backdrop = view.findViewById(R.id.backdrop);
        if (backdrop != null) backdrop.setOnClickListener(v -> dismiss());

        View btnClose = view.findViewById(R.id.btn_cancel_close);
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        View btnConfirm = view.findViewById(R.id.btn_confirm_cancel);
        if (btnConfirm != null) btnConfirm.setOnClickListener(v -> {
            if (listener != null) listener.onConfirmCancel();
            dismiss();
        });
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

