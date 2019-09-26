package com.senjuid.location.util;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class LocationHelper {
    private static final String TAG = "LocationHelper";


    private final long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private final long FASTEST_INTERVAL = 2000; /* 2 sec */
    private static final int REQUEST_CHECK_SETTINGS = 231;


    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private Activity activity;

    private LocationHelperListener locationHelperListener;

    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            if(!locationAvailability.isLocationAvailable()){
                stopLocationUpdates();

                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        locationHelperListener.onLocationUpdated(location);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        locationHelperListener.onLocationUpdated(null);
                    }
                });
            }
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
            stopLocationUpdates();
            locationHelperListener.onLocationUpdated(locationResult.getLastLocation());
        }
    };

    public LocationHelper(Activity activity, LocationHelperListener locationHelperListener){
        this.activity = activity;
        this.locationHelperListener = locationHelperListener;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
        settingsClient = LocationServices.getSettingsClient(activity);
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

    public void startUpdateLocation(){

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
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);

                                    locationHelperListener.needChangeToHighAccuracy();
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Please open your location settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private LocationSettingsRequest buildLocationSettingsRequest(LocationRequest locationRequest) {
        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        return builder.build();
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

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        if(requestCode == REQUEST_CHECK_SETTINGS){
            locationHelperListener.onChangeToHighAccuracy(resultCode == Activity.RESULT_OK);
        }
    }

    public interface LocationHelperListener{
        void onLocationUpdated(Location location);
        void needChangeToHighAccuracy();
        void onChangeToHighAccuracy(boolean changed);
    }
}
