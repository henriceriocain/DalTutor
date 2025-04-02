package com.example.csci3130group1.ui.manage_preferences;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.csci3130group1.R;
import com.example.csci3130group1.TutorialDetailsActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
//class is for showing the notifications on the very top
//firebase messaging service from the build.gradle file
//runs in the background
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    //creating a token, registering with the firebase messaging service
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM Token", "Token: " + token);
    }

    //main method
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d("message received", "received" + message);
        // If the notification message received is null, return. safety check
        if (message.getNotification() == null) {
            return;
        }

        // Extract fields from the notification message.
        final String title = message.getNotification().getTitle();
        final String body = message.getNotification().getBody();

        //getting the data
        final Map<String, String> data = message.getData();
        Log.d("NotificationReceived", "Title: " + title + ", Body: " + body + ", Data: " + data);

        // Create an intent to start activity when the notification is clicked.
        Intent intent = new Intent(this, TutorialDetailsActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        //based on the flag, the notification will be displayed
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 10, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        // Create a notification that will be displayed in the notification tray.
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, "Mathematics")
                        .setSmallIcon(R.drawable.app_icon)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
                new NotificationCompat.Builder(this, "Physics")
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
                new NotificationCompat.Builder(this, "Chemistry")
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
                new NotificationCompat.Builder(this, "Chemistry")
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
                new NotificationCompat.Builder(this, "Biology")
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
                new NotificationCompat.Builder(this, "Computer Science")
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
                new NotificationCompat.Builder(this, "English")
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
                new NotificationCompat.Builder(this, "History")
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Add the intent to the notification.
        notificationBuilder.setContentIntent(pendingIntent);

        // Notification manager to display the notification.
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        int id = (int) System.currentTimeMillis();
        if (notificationManager == null) {
            Log.e("NotificationError", "NotificationManager is null.");
            return;
        }

        // If the build version is greater than, put the notification in a channel.
        //grouping the notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mathChannel = new NotificationChannel("Mathematics", "Mathematics", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mathChannel);
            NotificationChannel csChannel = new NotificationChannel("Computer Science", "Computer Science", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(csChannel);
            NotificationChannel historyChannel = new NotificationChannel("History", "History", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(historyChannel);
            NotificationChannel englishChannel = new NotificationChannel("English", "English", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(englishChannel);
            NotificationChannel chemistryChannel = new NotificationChannel("Chemistry", "Chemistry", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(chemistryChannel);
            NotificationChannel physicsChannel = new NotificationChannel("Physics", "Physics", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(physicsChannel);
            NotificationChannel biologyChannel = new NotificationChannel("Biology", "Biology", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(biologyChannel);
        }

        // Display the push notification.
        notificationManager.notify(id, notificationBuilder.build());
    }
}

