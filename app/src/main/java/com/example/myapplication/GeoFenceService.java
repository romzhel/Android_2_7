package com.example.myapplication;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeoFenceService extends IntentService {
    private static final String SERVICE_NAME = "GeoFenceService";
    private static final String CHANNEL_ID = "myNotificationChannelId";
    private int notificationId = 0;

    public GeoFenceService() {
        super(SERVICE_NAME);
        Log.d(MainActivity.TAG, "geo fence intent service constructor");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Log.d(MainActivity.TAG, "geo fence intent service onCreate");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(MainActivity.TAG, "geo fence intent service onHandleIntent");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        int transitionType = geofencingEvent.getGeofenceTransition();
        notifyGeofence(transitionType);
    }

    private void notifyGeofence(final int transitionType) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("событие гео зоны")
                .setContentText(getTransitionTypeString(transitionType))
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(GeoFenceService.this);
        notificationManager.notify(notificationId++, notificationBuilder.build());

        Log.d(MainActivity.TAG, "notify " + getTransitionTypeString(transitionType));
    }

    private String getTransitionTypeString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "enter";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "exit";
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "inside";
            default:
                return "unknown";
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "myChannelName";
            String description = "MyDescription";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
