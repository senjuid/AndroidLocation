package com.senjuid.location;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class GDLocationService extends IntentService {
    private static final String TAG = "GDLocationService";
    
    public static final String INTENT_LOCATION_VALUE = "currentLocation";
    public static final String INTENT_LOCATION_ERROR = "currentLocationError";
    public static final String MY_LOCATION = "MY_CURRENT_LOCATION";

    private final long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private final long FASTEST_INTERVAL = 2000; /* 2 sec */


    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;

    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            Log.d(TAG, "onLocationAvailability: "+locationAvailability.toString());
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Log.d(TAG, "onLocationResult: " + locationResult.toString());
            handleUpdateLocation(locationResult.getLastLocation());
        }
    };

    // constructor
    public GDLocationService() {
        super("LOCATION_SERVICE");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        startUpdateLocation();
    }

    private void startUpdateLocation(){
        locationRequest = createLocationRequest();
        locationSettingsRequest = buildLocationSettingsRequest(locationRequest);
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // send broadcast failure
                    Intent intent = new Intent(MY_LOCATION);
                    intent.putExtra(INTENT_LOCATION_ERROR,
                            TextUtils.isEmpty(e.getLocalizedMessage())?"Location settings error":e.getLocalizedMessage());
                    sendBroadcast(intent);
                }
            });
    }

    private LocationRequest createLocationRequest() {
        // Create the location request to start receiving updates
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        /*
         * PRIORITIES
         * PRIORITY_BALANCED_POWER_ACCURACY -
         * PRIORITY_HIGH_ACCURACY -
         * PRIORITY_LOW_POWER -
         * PRIORITY_NO_POWER -
         * */
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private LocationSettingsRequest buildLocationSettingsRequest(LocationRequest locationRequest) {
        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        return builder.build();
    }


    private void handleUpdateLocation(Location location){
        Intent intent = new Intent(MY_LOCATION);
        intent.putExtra(INTENT_LOCATION_VALUE, location);
        sendBroadcast(intent);

        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        //stop location updates when  is no longer active
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "stopLocationUpdates: Successful");
                            } else {
                                Log.d(TAG, "stopLocationUpdates: " + Log.getStackTraceString(task.getException()));
                            }
                        }
                    });
        }
    }
}
