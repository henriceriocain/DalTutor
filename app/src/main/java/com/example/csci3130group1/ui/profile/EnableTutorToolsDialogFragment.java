package com.example.csci3130group1.ui.profile;

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

/**
 * Confirmation modal shown when enabling Tutor Tools, matching the Create Thread modal style.
 * Offers Revert (keep disabled) and Enable actions.
 */
public class EnableTutorToolsDialogFragment extends DialogFragment {

    public interface Listener {
        void onEnableConfirmed();
        void onEnableRejected();
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
        return inflater.inflate(R.layout.dialog_enable_tutor_tools_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Backdrop click = reject
        View backdrop = view.findViewById(R.id.backdrop);
        if (backdrop != null) {
            backdrop.setOnClickListener(v -> {
                notifyRejected();
                dismiss();
            });
        }

        View btnRevert = view.findViewById(R.id.btn_revert);
        if (btnRevert != null) {
            btnRevert.setOnClickListener(v -> {
                notifyRejected();
                dismiss();
            });
        }

        View btnEnable = view.findViewById(R.id.btn_enable);
        if (btnEnable != null) {
            btnEnable.setOnClickListener(v -> {
                notifyConfirmed();
                dismiss();
            });
        }
    }

    private void notifyConfirmed() {
        if (getParentFragment() instanceof Listener) {
            ((Listener) getParentFragment()).onEnableConfirmed();
        }
    }

    private void notifyRejected() {
        if (getParentFragment() instanceof Listener) {
            ((Listener) getParentFragment()).onEnableRejected();
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

