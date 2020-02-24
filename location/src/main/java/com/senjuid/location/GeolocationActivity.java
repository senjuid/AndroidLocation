package com.senjuid.location;

import android.Manifest;
import android.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.senjuid.location.util.BaseActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class GeolocationActivity extends BaseActivity {

    public static Integer PERMISSIONS_REQUEST_CODE = 1;
    public static Integer PERMISSIONS_REQUEST_SETTINGS = 2;

    private static final int REQUEST_CHECK_SETTINGS = 231;

    AndroidPermissions mPermissions;

    LocationManager locationManager;

    private GoogleMap mMap;

    int mHeight;

    TextView textView_oops_location;
    TextView textView_wrong_location;
    TextView tvSearching;

    TextView textView_location_maps_found_title;
    TextView tvAccuracy;
    Button button_location_maps_found_yes;
    Button button_location_maps_found_refresh;
    Button button_solution_location;

    View layoutLocationFound;
    View layoutLocationNotFound;

    GeolocationViewModel geolocationViewModel;

    // Extras
    String workLocationData;
    double workLat;
    double workLon;

    // Label
    String label1;
    String label2;

    OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;

            workLat = -6.174793; // default lat
            workLon = 106.827144; // default lon
            if(workLocationData != null){
                try{
                    JSONObject data = new JSONObject(workLocationData);
                    JSONArray locArray = data.getJSONArray("data");

                    if(locArray != null && locArray.length()> 0){
                        for(int i=0;i<locArray.length();i++){
                            addCompanyLocation(locArray.getJSONObject(i));

                            // set start location
                            if(i == 0){
                                workLat = locArray.getJSONObject(i).optDouble("work_lat");
                                workLon = locArray.getJSONObject(i).optDouble("work_lon");
                            }
                        }
                    }
                }catch (JSONException je){}
            }

            LatLng sydney = new LatLng(workLat, workLon);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16.0f));

            if (ActivityCompat.checkSelfPermission(GeolocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(GeolocationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                setupPermissions();
                return;
            }

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            mHeight = displayMetrics.heightPixels;

            // get location update
            myLocation();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        label1 = getIntent().getStringExtra("message1");
        label2 = getIntent().getStringExtra("message2");
        workLocationData = getIntent().getStringExtra("data");

        // check google api available
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        if(googleApiAvailability.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS){
            googleApiAvailability.getErrorDialog(this, 404, 200, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                    finish();
                }
            }).show();
            return;
        }

        // create view model
        geolocationViewModel = ViewModelProviders
                .of(this, new GeolocationViewModelFactory(getApplicationContext()))
                .get(GeolocationViewModel.class);

        initComponent();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(onMapReadyCallback);

        ImageButton locationFoundRefresh = findViewById(R.id.button_center_location);
        locationFoundRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupPermissions();
            }
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // observe live data
        observeLiveData();

        // hide loading
        showHideLoading(true);
    }

    // Add company location  marker and radius
    private void addCompanyLocation(JSONObject data) throws JSONException {
        LatLng companyLocation = new LatLng(data.getDouble("work_lat"), data.getDouble("work_lon"));

        // Add circle
        if (data.getDouble("work_radius") > 0) { // add circle radius only if geo fencing active

            //add marker
            mMap.addMarker(new MarkerOptions()
                    .position(companyLocation)
                    .title(getString(R.string.your_company))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_company_marker)));

            int fillColor = 0x4400FF00;
            double radius = data.getDouble("work_radius") * 1000;
            mMap.addCircle(new CircleOptions()
                    .center(companyLocation)
                    .radius(radius)
                    .strokeColor(Color.GREEN)
                    .strokeWidth(2f)
                    .fillColor(fillColor));
        }
    }

    private void observeLiveData() {
        // observe location update
        geolocationViewModel.location.observe(this, new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location location) {
                setMyLocation(location);
            }
        });

        // observe high accuracy
        geolocationViewModel.resolvableApiException.observe(this, new Observer<ResolvableApiException>() {
            @Override
            public void onChanged(@Nullable ResolvableApiException e) {
                if (e != null) {
                    try {
                        e.startResolutionForResult(GeolocationActivity.this, REQUEST_CHECK_SETTINGS);

                        // show message
                        showHideLoading(false);
                        if(getIntent().getStringExtra("message2") != null) {
                            textView_wrong_location.setText(getIntent().getStringExtra("message2"));
                        } else {
                            textView_wrong_location.setText(getString(R.string.str_mod_loc_high_accuracy));
                        }
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS && resultCode == RESULT_OK) {
            geolocationViewModel.startUpdateLocation();
        } else {
            showHideLoading(false);
            setMyLocation(null);
        }
    }

    public void myLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        showHideLoading(true);
        geolocationViewModel.startUpdateLocation();
    }


    public void setMyLocation(Location location) {
        if (location == null || mMap == null) {
            hideComponent();
            return;
        }

        // My location marker
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        // show map point
        Point mapPoint = mMap.getProjection().toScreenLocation(myLocation);
        mapPoint.set(mapPoint.x, mapPoint.y); // change these values as you need , just hard coded a value if you want you can give it based on a ratio like using DisplayMetrics  as well
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mMap.getProjection().fromScreenLocation(mapPoint), 16.0f));
        mMap.setMyLocationEnabled(true);

        // set accuracy
        tvAccuracy.setText(geolocationViewModel.formatAccuracy(getString(R.string.str_accuracy), location));

        showComponent();
    }

    private void initComponent() {
        textView_oops_location = findViewById(R.id.textView_oops_location);
        textView_wrong_location = findViewById(R.id.textView_wrong_location);
        tvSearching = findViewById(R.id.tv_state);
        button_solution_location = findViewById(R.id.button_solution_location);
        layoutLocationFound = findViewById(R.id.layout_location_found);
        layoutLocationNotFound = findViewById(R.id.layout_location_not_found);
        tvAccuracy = findViewById(R.id.tv_accuracy);

        textView_location_maps_found_title = findViewById(R.id.textView_location_maps_found_title);
        if(getIntent().getStringExtra("message1") != null) {
            textView_location_maps_found_title.setText(getIntent().getStringExtra("message1"));
        }

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

    private void showHideLoading(boolean loading) {
        if (loading) {
            tvSearching.setText(getString(R.string.str_mod_loc_searching));
            tvSearching.setVisibility(View.VISIBLE);
            layoutLocationNotFound.setVisibility(View.GONE);
            layoutLocationFound.setVisibility(View.GONE);
        } else {
            tvSearching.setVisibility(View.GONE);
        }
    }

    private void hideComponent() {
        layoutLocationNotFound.setVisibility(View.VISIBLE);
        layoutLocationFound.setVisibility(View.GONE);
        tvSearching.setVisibility(View.GONE);
    }

    private void showComponent() {
        layoutLocationNotFound.setVisibility(View.GONE);
        layoutLocationFound.setVisibility(View.VISIBLE);
        tvSearching.setVisibility(View.GONE);
    }

    private void onRefreshButtonPressed() {
        setupPermissions();
    }

    private void onYesButtonPressed() {
        Location location = geolocationViewModel.location.getValue();

        if (location != null) { // make sure location not null
            onYesButtonPressed(location.getLatitude(), location.getLongitude(), "");
            finish();
        }
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

    public abstract void onYesButtonPressed(Double latitude, Double longitude, String address);
}
