package com.senjuid.location;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

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
    public MutableLiveData<String> address = new MutableLiveData<>();


    public GeolocationViewModel(Context appContext){
        this.wrContext = new WeakReference<>(appContext);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext);
        settingsClient = LocationServices.getSettingsClient(appContext);
        address.setValue("");
    }


    // MARK: Public Functions
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

    public void loadAddressFromLocation(Location location){
        if(location != null){
            new GetAddressTask(this).execute(location);
        }else {
            address.postValue("");
        }
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
                stopLocationUpdates();

                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        GeolocationViewModel.this.location.postValue(location);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        GeolocationViewModel.this.location.postValue(null);
                    }
                });
            }
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
            stopLocationUpdates();
            GeolocationViewModel.this.location.postValue(locationResult.getLastLocation());
        }
    };

    @Override
    protected void onCleared() {
        stopLocationUpdates();
        super.onCleared();
    }

    private static class GetAddressTask extends AsyncTask<Location, Void, String> {

        WeakReference<GeolocationViewModel> wrViewModel;

        GetAddressTask(GeolocationViewModel viewModel){
            wrViewModel = new WeakReference<>(viewModel);
        }

        @Override
        protected String doInBackground(Location... locations) {
            try {
                Context context = wrViewModel.get().wrContext.get();
                Location location = locations[0];
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                String _address = addresses.get(0).getAddressLine(0);
                if(!TextUtils.isEmpty(_address)) {
                    return _address;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String address) {
            wrViewModel.get().address.postValue(address);
        }
    }
}
