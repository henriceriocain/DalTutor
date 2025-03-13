package com.example.csci3130group1.ui.search_for_tutorials;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.csci3130group1.R;

import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;

import java.util.List;

public class TutorialAdapter extends BaseAdapter {

    private Context context;
    private List<Tutorial> tutorials;

    public TutorialAdapter(Context context, List<Tutorial> tutorials) {
        this.context = context;
        this.tutorials = tutorials;
    }

    @Override
    public int getCount() {
        return tutorials.size();
    }

    @Override
    public Object getItem(int position) {
        return tutorials.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_tutorial, parent, false);
        }

        TextView title = convertView.findViewById(R.id.tutorialTitle);
        TextView details = convertView.findViewById(R.id.tutorialDetails);
        ImageView mapIcon = convertView.findViewById(R.id.mapIcon);

        Tutorial tutorial = tutorials.get(position);
        title.setText(tutorial.getTitle());
        details.setText("Location: " + tutorial.getLocation() + " | Fee: $" + tutorial.getFee() + " | " + tutorial.getDuration() + " hrs");

        return convertView;
    }
}