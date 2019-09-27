package com.senjuid.location;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.senjuid.location.util.BaseActivity;
import com.senjuid.location.util.LocationHelper;

public abstract class GeolocationActivity extends BaseActivity  {

    public static Integer PERMISSIONS_REQUEST_CODE = 1;
    public static Integer PERMISSIONS_REQUEST_SETTINGS = 2;

    AndroidPermissions mPermissions;

    LocationManager locationManager;

    private GoogleMap mMap;

    int mHeight;

    TextView textView_oops_location;
    TextView textView_wrong_location;
    TextView tvSearching;

    TextView textView_location_maps_found_title;
    TextView textView_location_maps_found_description;
    Button button_location_maps_found_yes;
    Button button_location_maps_found_refresh;
    Button button_solution_location;

    Double mLongitude;
    Double mLatitude;
    String mAddress;

    View layoutLocationFound;
    View layoutLocationNotFound;

    LocationHelper locationHelper;

    OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;

            LatLng sydney = new LatLng(-6.1753924, 106.8271528);
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


            Location location = getIntent().getParcelableExtra("current_location");
            if(location != null){
                setMyLocation(location);
            }else {
                myLocation();
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
        mapFragment.getMapAsync(onMapReadyCallback);

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

        locationHelper = new LocationHelper(this, new LocationHelper.LocationHelperListener() {
            @Override
            public void onLocationUpdated(Location location) {
                showHideLoading(false);
                setMyLocation(location);
            }

            @Override
            public void onChangeToHighAccuracy(boolean changed) {
                if(changed){
                    showHideLoading(true);
                    locationHelper.startUpdateLocation();
                }else{
                    showHideLoading(false);
                    setMyLocation(null);
                }
            }

            @Override
            public void needChangeToHighAccuracy() {
                showHideLoading(false);

                tvSearching.setVisibility(View.VISIBLE);
                tvSearching.setText(getString(R.string.str_mod_loc_high_accuracy));
            }
        });

        showHideLoading(true);
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
    protected void onDestroy() {
        locationHelper.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        locationHelper.onActivityResult(requestCode, resultCode, data);
    }

    public void  myLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        showHideLoading(true);
        locationHelper.startUpdateLocation();
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
            GeoLocator geoLocator = new GeoLocator(getApplicationContext(), GeolocationActivity.this);
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
        tvSearching = findViewById(R.id.tv_state);
        button_solution_location = findViewById(R.id.button_solution_location);
        layoutLocationFound = findViewById(R.id.layout_location_found);
        layoutLocationNotFound = findViewById(R.id.layout_location_not_found);

        textView_location_maps_found_title = findViewById(R.id.textView_location_maps_found_title);
        textView_location_maps_found_description = findViewById(R.id.textView_location_maps_found_description);
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

    private void showHideLoading(boolean loading){
        if(loading) {
            tvSearching.setVisibility(View.VISIBLE);
            layoutLocationNotFound.setVisibility(View.GONE);
            layoutLocationFound.setVisibility(View.GONE);
        }else{
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
        onYesButtonPressed(mLatitude, mLongitude, mAddress);
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

    public abstract void onYesButtonPressed(Double latitude, Double longitude, String address);


//    public static Integer PERMISSIONS_REQUEST_CODE = 1;
//    public static Integer PERMISSIONS_REQUEST_SETTINGS = 2;
//    public static Integer REQUEST_MAPS = 3;
//
//    AndroidPermissions mPermissions;
//
//    LocationManager locationManager;
//
//    LottieAnimationView searchingLocationLottie;
//    TextView searchingLocationTitle;
//
//    LottieAnimationView locationFoundLottie;
//    TextView locationFoundTitle;
//    TextView locationFoundDescription;
//    ImageView imageLocationFoundDescription;
//    TextView locationFoundQuestion;
//    Button locationFoundYes;
//    Button locationFoundRefresh;
//    View locationFoundLeft;
//    View locationFoundRight;
//    TextView locationFoundOr;
//    TextView locationFoundGoogleMaps;
//    ImageView imageLocationFoundGoogleMaps;
//
//    Double mLongitude;
//    Double mLatitude;
//    String mAddress;
//
//    String url = "https://maps.google.com/maps/api/staticmap?zoom=18&size=600x1750&sensor=false";
//
//    LocationHelper locationHelper;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_geolocation);
//
//        ApplicationInfo app = null;
//        try {
//            app = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
//            Bundle bundle = app.metaData;
//            String key = "&key=" + bundle.getString("com.google.android.geo.API_KEY");
//            System.out.println("KEY " + key);
//            url = url + key;
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        locationHelper = new LocationHelper(this, new LocationHelper.LocationHelperListener() {
//            @Override
//            public void onLocationUpdated(Location location) {
//                Intent i = new Intent(GeolocationActivity.this, MapsActivity.class);
//                i.putExtra("current_location", location);
//                startActivityForResult(i, REQUEST_MAPS);
//            }
//
//            @Override
//            public void onChangeToHighAccuracy(boolean changed) {
//                if(changed)
//                    locationHelper.startUpdateLocation();
//                else
//                    finish();
//            }
//        });
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        initialComponent();
//        setupPermissions();
//    }
//
//    private void setupPermissions() {
//        mPermissions = new AndroidPermissions(this,
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.CAMERA
//        );
//        if (mPermissions.checkPermissions()) {
//            if (checkLocation()) {
//                loadLocation();
//            }
//        } else {
//            mPermissions.requestPermissions(PERMISSIONS_REQUEST_CODE);
//        }
//    }
//
//    // Check GPS is active or not
//    private Boolean isLocationEnabled() {
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        int locationMode;
//        try {
//            locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
//        } catch (Settings.SettingNotFoundException e) {
//            locationMode = 1;
//        }
//        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && locationMode == 3;
//    }
//
//    private Boolean checkLocation() {
//        if (!isLocationEnabled())
//            showAlert();
//        return isLocationEnabled();
//    }
//
//
//    private void showAlert() {
//        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
//        dialog
//                .setTitle("Enable Location")
//                .setMessage("Please verify you've switched Location to \"On\". Also, set your Location Mode to High Accuracy")
//                .setCancelable(false)
//                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                        startActivityForResult(myIntent, PERMISSIONS_REQUEST_SETTINGS);
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
////                        finish();
//                    }
//                })
//                .show();
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSIONS_REQUEST_CODE) {
//            if (mPermissions.areAllRequiredPermissionsGranted(permissions, grantResults)) {
//                setupPermissions();
//            } else {
//                onInsufficientPermissions();
//            }
//        } else if (requestCode == PERMISSIONS_REQUEST_SETTINGS) {
//            setupPermissions();
//        }
//    }
//
//    private void onInsufficientPermissions() {
//        finish();
//    }
//
//    private void loadLocation() {
//        hideComponent();
//        locationHelper.startUpdateLocation();
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_MAPS) {
//            if (resultCode == RESULT_OK) {
//                assert data != null;
//                mLatitude = Double.valueOf(data.getStringExtra("latitude"));
//                mLongitude = Double.valueOf(data.getStringExtra("longitude"));
//                mAddress = data.getStringExtra("address");
//                onYesButtonPressed(mLatitude, mLongitude, mAddress);
//                finish();
//            }else{
//                finish();
//            }
//        } else if (requestCode == PERMISSIONS_REQUEST_SETTINGS) {
//            LocationRequest mLocationRequest = LocationRequest.create();
//            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        }
//
//        locationHelper.onActivityResult(requestCode, resultCode, data);
//    }
//
//    private void initialComponent() {
//        searchingLocationLottie = findViewById(R.id.lottie_animation_searching_location);
//        searchingLocationTitle = findViewById(R.id.textView_searching_location);
//
//        locationFoundLottie = findViewById(R.id.lottie_animation_location_found);
//        locationFoundTitle = findViewById(R.id.textView_location_found_title);
//        locationFoundDescription = findViewById(R.id.textView_location_found_description);
//        imageLocationFoundDescription = findViewById(R.id.imageView_location_found_description);
//        locationFoundQuestion = findViewById(R.id.textView_location_found_question);
//        locationFoundYes = findViewById(R.id.button_location_found_yes);
//        locationFoundRefresh = findViewById(R.id.button_location_found_refresh);
//        locationFoundLeft = findViewById(R.id.view_location_found_left);
//        locationFoundRight = findViewById(R.id.view_location_found_right);
//        locationFoundOr = findViewById(R.id.textView_location_found_or);
//        locationFoundGoogleMaps = findViewById(R.id.textView_location_found_google_maps);
//        imageLocationFoundGoogleMaps = findViewById(R.id.image__location_found_google_maps);
//
//        locationFoundRefresh.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                setupPermissions();
//            }
//        });
//
//        locationFoundYes.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                onYesButtonPressed(mLatitude, mLongitude, mAddress);
//                finish();
//            }
//        });
//
//        imageLocationFoundDescription.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String geo = "geo:" + mLatitude + "," + mLongitude;
//                Uri gmmIntentUri = Uri.parse(geo);
//                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//                mapIntent.setPackage("com.google.android.apps.maps");
//                if (mapIntent.resolveActivity(getPackageManager()) != null) {
//                    startActivity(mapIntent);
//                }
//            }
//        });
//
//        locationFoundGoogleMaps.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String geo = "geo:" + mLatitude + "," + mLongitude;
//                Uri gmmIntentUri = Uri.parse(geo);
//                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//                mapIntent.setPackage("com.google.android.apps.maps");
//                if (mapIntent.resolveActivity(getPackageManager()) != null) {
//                    startActivity(mapIntent);
//                }
//            }
//        });
//
//        imageLocationFoundGoogleMaps.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String geo = "geo:" + mLatitude + "," + mLongitude;
//                Uri gmmIntentUri = Uri.parse(geo);
//                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//                mapIntent.setPackage("com.google.android.apps.maps");
//                if (mapIntent.resolveActivity(getPackageManager()) != null) {
//                    startActivity(mapIntent);
//                }
//            }
//        });
//    }
//
//    private void showComponent() {
//        searchingLocationLottie.setVisibility(View.GONE);
//        searchingLocationTitle.setVisibility(View.GONE);
//
//        locationFoundLottie.setVisibility(View.VISIBLE);
//        locationFoundTitle.setVisibility(View.VISIBLE);
////        locationFoundDescription.setVisibility(View.VISIBLE);
//        imageLocationFoundDescription.setVisibility(View.VISIBLE);
//        locationFoundQuestion.setVisibility(View.VISIBLE);
//        locationFoundYes.setVisibility(View.VISIBLE);
//        locationFoundRefresh.setVisibility(View.VISIBLE);
//        locationFoundLeft.setVisibility(View.VISIBLE);
//        locationFoundRight.setVisibility(View.VISIBLE);
//        locationFoundOr.setVisibility(View.VISIBLE);
//        locationFoundGoogleMaps.setVisibility(View.VISIBLE);
//        imageLocationFoundGoogleMaps.setVisibility(View.VISIBLE);
//    }
//
//    private void hideComponent() {
//        searchingLocationLottie.setVisibility(View.VISIBLE);
//        searchingLocationTitle.setVisibility(View.VISIBLE);
//
//        locationFoundLottie.setVisibility(View.GONE);
//        locationFoundTitle.setVisibility(View.GONE);
//        locationFoundDescription.setVisibility(View.GONE);
//        imageLocationFoundDescription.setVisibility(View.GONE);
//        locationFoundQuestion.setVisibility(View.GONE);
//        locationFoundYes.setVisibility(View.GONE);
//        locationFoundRefresh.setVisibility(View.GONE);
//        locationFoundLeft.setVisibility(View.GONE);
//        locationFoundRight.setVisibility(View.GONE);
//        locationFoundOr.setVisibility(View.GONE);
//        locationFoundGoogleMaps.setVisibility(View.GONE);
//        imageLocationFoundGoogleMaps.setVisibility(View.GONE);
//    }
//
//    public abstract void onYesButtonPressed(Double latitude, Double longitude, String address);

}
