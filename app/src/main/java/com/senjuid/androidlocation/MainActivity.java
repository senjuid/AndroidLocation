package com.senjuid.androidlocation;

import android.os.Bundle;
import android.widget.Toast;

import com.senjuid.location.GeolocationActivity;
import com.senjuid.location.util.LocaleHelper;

public class MainActivity extends GeolocationActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleHelper.setLocale(this, "ko");
    }

    @Override
    public void onYesButtonPressed(Double latitude, Double longitude, String address) {
        Toast.makeText(this, latitude + "," + longitude + " " + address, Toast.LENGTH_SHORT).show();
    }

}
