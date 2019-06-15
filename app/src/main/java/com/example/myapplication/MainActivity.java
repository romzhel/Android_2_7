package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String TAG = "myApp";
    public static final int PERMISSION_REQUEST_CODE = 10;
    public static final String GEOFENCE_REQUEST_ID = "1";
    private LocationManager locationManager;
    private String provider;
    private TextView providerText;
    private TextView accuracyText;
    private TextView latitudeText;
    private TextView longitudeText;

    private GoogleApiClient googleApiClient;
    private Geofence geofence;
    private double latitude;
    private double longitude;
    private static final float RADIUS_BY_DEFAULT = 1000;
    private static final int DURATION_BY_DEFAULT = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGui();
        initLocationAndPermissions();
    }

    private void initLocationAndPermissions() {
        int finePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (finePermission == PackageManager.PERMISSION_GRANTED && coarsePermission == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        } else {
            requestLocationPermission();
        }
    }

    private void initGui() {
        Log.d(TAG, "init GUI");
        providerText = (TextView) findViewById(R.id.provider);
        accuracyText = (TextView) findViewById(R.id.accuracy);
        latitudeText = (TextView) findViewById(R.id.latitude);
        longitudeText = (TextView) findViewById(R.id.longitude);

        findViewById(R.id.btn_set_geofence).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createGoogleApiClient();
            }
        });
    }

    private void requestLocation() {
        Log.d(TAG, "request location");

        int finePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (finePermission != PackageManager.PERMISSION_GRANTED && coarsePermission != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);

        provider = locationManager.getBestProvider(criteria, true);
    }

    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            accuracyText.setText(String.valueOf(location.getAccuracy()));
            latitudeText.setText(String.valueOf(latitude));
            longitudeText.setText(String.valueOf(longitude));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "status changed to " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "provider enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "provider disabled");
        }
    };

    private void requestLocationPermission() {
        Log.d(TAG, "request permission");
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "request permission result");

        if (requestCode == PERMISSION_REQUEST_CODE) {
            Log.d(TAG, "request code = our code");

            for (int i = 0; i < permissions.length; i++) {
                Log.d(TAG, permissions[i] + " = " + grantResults[i]);
            }

            requestLocation();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity onStart");

        if (provider != null) {
            providerText.setText(provider);

            locationManager.requestLocationUpdates(provider, 1000, 10, listener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity onStop");

        locationManager.removeUpdates(listener);
    }

    private void createGoogleApiClient() {
        geofence = new Geofence.Builder()
                .setRequestId(GEOFENCE_REQUEST_ID)
                .setTransitionTypes(
                        GeofencingRequest.INITIAL_TRIGGER_ENTER |
                                GeofencingRequest.INITIAL_TRIGGER_DWELL |
                                GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .setCircularRegion(latitude, longitude, RADIUS_BY_DEFAULT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(DURATION_BY_DEFAULT)
                .build();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.d(TAG, "geo fence connection failed listener");
                    }
                })
                .build();
        googleApiClient.connect();
        Log.d(TAG, "geo fence connect to googleApiClient (" + latitude + ", " + longitude + ")");
    }

    private GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

            builder.setInitialTrigger(
                    GeofencingRequest.INITIAL_TRIGGER_ENTER |
                            GeofencingRequest.INITIAL_TRIGGER_DWELL |
                            GeofencingRequest.INITIAL_TRIGGER_EXIT);
            builder.addGeofence(geofence);
            GeofencingRequest geofencingRequest = builder.build();

            Intent geoService = new Intent(MainActivity.this, GeoFenceService.class);
            PendingIntent pendingIntent = PendingIntent.getService(MainActivity.this, 0, geoService,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            GeofencingClient geofencingClient = LocationServices.getGeofencingClient(MainActivity.this);
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "geofence added to geofencing client successfully");
                        }
                    })
                    .addOnFailureListener(MainActivity.this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "geofence not added to geofencing client, reason - " + e.getMessage());
                        }
                    });
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "connection suspended");
        }
    };
}
