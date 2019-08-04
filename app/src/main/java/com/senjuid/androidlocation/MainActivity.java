package com.senjuid.androidlocation;

import android.widget.Toast;

import com.senjuid.location.GeolocationActivity;

public class MainActivity extends GeolocationActivity {

    @Override
    public void onYesButtonPressed(Double latitude, Double longitude, String address) {
        Toast.makeText(this, latitude + "," + longitude + " " + address, Toast.LENGTH_SHORT).show();
    }

}
