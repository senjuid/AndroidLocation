package com.senjuid.androidlocation;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.senjuid.location.util.LocaleHelper;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set language
        LocaleHelper.setLocale(this, "in");

        // intent with bundle
        Intent i = new Intent(this, MapActivity.class);
        String dataDummy = "{data:[{work_lat: -6.283693, work_lon: 106.725430, work_radius: 0.4 },{work_lat: -6.175110, work_lon: 106.865036, work_radius: 0.5 }]}";
        i.putExtra("data", dataDummy);
        startActivity(i);
        finish();

//        i.putExtra("work_lat", -6.1753924);
//        i.putExtra("work_lon", 106.8271528);
//        i.putExtra("work_radius", 500);

    }

}

