package com.senjuid.androidlocation;

import android.widget.Toast;

import com.senjuid.location.GeolocationActivity;

public class MapActivity extends GeolocationActivity {
    @Override
    public void onYesButtonPressed(Double latitude, Double longitude, String address) {
        Toast.makeText(this, "Lat: "+latitude+", Lon: "+longitude, Toast.LENGTH_SHORT).show();
    }
}
