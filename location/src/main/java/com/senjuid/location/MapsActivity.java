package com.senjuid.location;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;

import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static Integer PERMISSIONS_REQUEST_CODE = 1;
    public static Integer PERMISSIONS_REQUEST_SETTINGS = 2;

    AndroidPermissions mPermissions;

    LocationManager locationManager;

    private GoogleMap mMap;

    int mHeight;

    TextView textView_oops_location;
    TextView textView_wrong_location;
    TextView textView_solution_location;

    TextView textView_location_maps_found_title;
    TextView textView_location_maps_found_description;
    TextView textView_location_maps_found_question;
    Button button_location_maps_found_yes;
    Button button_location_maps_found_refresh;
    Button button_solution_location;

    Double mLongitude;
    Double mLatitude;
    String mAddress;

    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Location location = intent.getParcelableExtra(GDLocationService.INTENT_LOCATION_VALUE);
                if(location != null) {
                    setMyLocation(location);
                    return;
                }

                String error = intent.getParcelableExtra(GDLocationService.INTENT_LOCATION_ERROR);
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        initComponent();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        ImageButton locationFoundRefresh = findViewById(R.id.button_center_location);
        locationFoundRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupPermissions();
            }
        });

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mLocationReceiver, new IntentFilter(GDLocationService.MY_LOCATION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mLocationReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng sydney = new LatLng(-6.1753924, 106.8271528);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16.0f));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mHeight = displayMetrics.heightPixels;


        Location location = getIntent().getParcelableExtra("current_location");
        if(location != null){
            setMyLocation(location);
        }else {
            myLocation();
        }

    }

    public void myLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Starting service
        Intent intent = new Intent(this, GDLocationService.class);
        startService(intent);
    }

    public void setMyLocation(Location location) {
        if(location == null){
            hideComponent();
            return;
        }

        Point mapPoint = mMap.getProjection().toScreenLocation(new LatLng(location.getLatitude(), location.getLongitude()));
        mapPoint.set(mapPoint.x, mapPoint.y + (mHeight / 5)); // change these values as you need , just hard coded a value if you want you can give it based on a ratio like using DisplayMetrics  as well
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mMap.getProjection().fromScreenLocation(mapPoint), 16.0f));

        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();

        try {
            GeoLocator geoLocator = new GeoLocator(getApplicationContext(), MapsActivity.this);
            textView_location_maps_found_description.setText(geoLocator.getAddress());
            mAddress = geoLocator.getAddress();
        }catch (Exception ex) {
            mAddress = "";
        }

        showComponent();
    }

    private void initComponent() {
        textView_oops_location = findViewById(R.id.textView_oops_location);
        textView_wrong_location = findViewById(R.id.textView_wrong_location);
//        textView_solution_location = findViewById(R.id.textView_solution_location);
        button_solution_location = findViewById(R.id.button_solution_location);

        textView_location_maps_found_title = findViewById(R.id.textView_location_maps_found_title);
        textView_location_maps_found_description = findViewById(R.id.textView_location_maps_found_description);
        textView_location_maps_found_question = findViewById(R.id.textView_location_maps_found_question);
        button_location_maps_found_yes = findViewById(R.id.button_location_maps_found_yes);
        button_location_maps_found_refresh = findViewById(R.id.button_location_maps_found_refresh);

        button_location_maps_found_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onYesButtonPressed();
            }
        });

        button_location_maps_found_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefreshButtonPressed();
            }
        });

        button_solution_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupPermissions();
            }
        });
    }

    private void hideComponent() {
        textView_oops_location.setVisibility(View.VISIBLE);
        textView_wrong_location.setVisibility(View.VISIBLE);
//        textView_solution_location.setVisibility(View.VISIBLE);
        button_solution_location.setVisibility(View.VISIBLE);

        textView_location_maps_found_title.setVisibility(View.GONE);
        textView_location_maps_found_description.setVisibility(View.GONE);
        textView_location_maps_found_question.setVisibility(View.GONE);
        button_location_maps_found_yes.setVisibility(View.GONE);
        button_location_maps_found_refresh.setVisibility(View.GONE);
    }

    private void showComponent() {
        textView_oops_location.setVisibility(View.GONE);
        textView_wrong_location.setVisibility(View.GONE);
//        textView_solution_location.setVisibility(View.GONE);
        button_solution_location.setVisibility(View.GONE);

        textView_location_maps_found_title.setVisibility(View.VISIBLE);
        textView_location_maps_found_description.setVisibility(View.VISIBLE);
        textView_location_maps_found_question.setVisibility(View.VISIBLE);
        button_location_maps_found_yes.setVisibility(View.VISIBLE);
        button_location_maps_found_refresh.setVisibility(View.VISIBLE);
    }

    private void onRefreshButtonPressed() {
        setupPermissions();
    }

    private void onYesButtonPressed() {
        Intent intent = getIntent();
        System.out.println(mLatitude);
        intent.putExtra("latitude", mLatitude.toString());
        intent.putExtra("longitude", mLongitude.toString());
        intent.putExtra("address", mAddress);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void setupPermissions() {
        mPermissions = new AndroidPermissions(this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        );
        if (mPermissions.checkPermissions()) {
            if (checkLocation()) {
                myLocation();
            }
        } else {
            mPermissions.requestPermissions(PERMISSIONS_REQUEST_CODE);
        }
    }

    // Check GPS is active or not
    private Boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private Boolean checkLocation() {
        if (!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }


    private void showAlert() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog
                .setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'. Please Enable Location to use this app")
                .setCancelable(false)
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(myIntent, PERMISSIONS_REQUEST_SETTINGS);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
//                        finish();
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (mPermissions.areAllRequiredPermissionsGranted(permissions, grantResults)) {
                setupPermissions();
            } else {
                onInsufficientPermissions();
            }
        } else if (requestCode == PERMISSIONS_REQUEST_SETTINGS) {
            setupPermissions();
        }
    }

    private void onInsufficientPermissions() {
        finish();
    }
}
