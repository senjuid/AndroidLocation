package com.senjuid.androidlocation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.senjuid.location.util.LocaleHelper;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set language
        LocaleHelper.setLocale(this, "in");

        // intent with bundle
        Intent i = new Intent(this, MapActivity.class);
        i.putExtra("work_lat", -6.283693);
        i.putExtra("work_lon", 106.725453);
        i.putExtra("work_radius", 100);
        startActivity(i);

        finish();
    }
}
