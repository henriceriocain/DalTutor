package com.example.csci3130group1;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import com.example.csci3130group1.ui.TutorialManagement.TutorialManagementFragment;

public class TutorialManagementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_management);

        // Load the TutorialManagementFragment
        if (savedInstanceState == null) {
            TutorialManagementFragment fragment = new TutorialManagementFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}