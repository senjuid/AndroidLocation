package com.senjuid.location;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.Log;

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

import java.lang.ref.WeakReference;

public class GeolocationViewModel extends ViewModel {

    private static final String TAG = "GeolocationViewModel";
    private final long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private final long FASTEST_INTERVAL = 2000; /* 2 sec */

    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private WeakReference<Context> wrContext;

    public MutableLiveData<ResolvableApiException> resolvableApiException = new MutableLiveData<>();
    public MutableLiveData<Location> location = new MutableLiveData<>();

    private boolean isFirstRequest = true;

    public GeolocationViewModel(Context appContext){
        this.wrContext = new WeakReference<>(appContext);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext);
        settingsClient = LocationServices.getSettingsClient(appContext);
    }


    // MARK: Public Functions
    public void startUpdateLocation(){
        if(isFirstRequest) {
            isFirstRequest = false;

            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        GeolocationViewModel.this.location.setValue(location);
                    }
                });
        }

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
                                ResolvableApiException exception = (ResolvableApiException) e;
                                resolvableApiException.postValue(exception);
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Please open your location settings.";
                                Log.e(TAG, errorMessage);
                        }
                    }
                });
    }

    public void stopLocationUpdates() {
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

    public String formatAccuracy(String wording, Location location){
        float accuracy;
        if(location != null){
            accuracy = location.getAccuracy();
            return String.format(wording, (int)accuracy);
        }
        return "";
    }

    // MARK: Private Functions
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


    // MARK: Private variable
    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            if(!locationAvailability.isLocationAvailable()){
//                stopLocationUpdates();

                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        GeolocationViewModel.this.location.setValue(location);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        GeolocationViewModel.this.location.setValue(null);
                    }
                });
            }
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
//            stopLocationUpdates();
            GeolocationViewModel.this.location.setValue(locationResult.getLastLocation());
        }
    };

    @Override
    protected void onCleared() {
        stopLocationUpdates();
        super.onCleared();
    }
}
