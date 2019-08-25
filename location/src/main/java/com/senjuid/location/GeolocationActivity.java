package com.senjuid.location;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.Image;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.LocationRequest;

public abstract class GeolocationActivity extends AppCompatActivity {

    public static Integer PERMISSIONS_REQUEST_CODE = 1;
    public static Integer PERMISSIONS_REQUEST_SETTINGS = 2;
    public static Integer REQUEST_MAPS = 3;

    AndroidPermissions mPermissions;

    LocationManager locationManager;

    LottieAnimationView searchingLocationLottie;
    TextView searchingLocationTitle;

    LottieAnimationView locationFoundLottie;
    TextView locationFoundTitle;
    TextView locationFoundDescription;
    ImageView imageLocationFoundDescription;
    TextView locationFoundQuestion;
    Button locationFoundYes;
    Button locationFoundRefresh;
    View locationFoundLeft;
    View locationFoundRight;
    TextView locationFoundOr;
    TextView locationFoundGoogleMaps;
    ImageView imageLocationFoundGoogleMaps;

    Double mLongitude;
    Double mLatitude;
    String mAddress;

    String url = "https://maps.google.com/maps/api/staticmap?zoom=18&size=600x1750&sensor=false";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geolocation);

        ApplicationInfo app = null;
        try {
            app = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;
            String key = "&key=" + bundle.getString("com.google.android.geo.API_KEY");
            url = url + key;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        initialComponent();
        setupPermissions();
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
                loadLocation();
            }
        } else {
            mPermissions.requestPermissions(PERMISSIONS_REQUEST_CODE);
        }
    }

    // Check GPS is active or not
    private Boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        int locationMode;
        try {
            locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            locationMode = 1;
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && locationMode == 3;
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
                .setMessage("Please verify you've switched Location to \"On\". Also, set your Location Mode to High Accuracy")
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

    private void loadLocation() {
        hideComponent();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    GeoLocator geoLocator = new GeoLocator(getApplicationContext(), GeolocationActivity.this);
                    mLongitude = geoLocator.getLongitude();
                    mLatitude = geoLocator.getLattitude();
                    mAddress = geoLocator.getAddress();
//                    locationFoundDescription.setText(geoLocator.getAddress());
//                    imageLocationFoundDescription

                    Glide
                            .with(getApplicationContext())
                            .load(url + "&center=" + mLatitude + "," + mLongitude + "&markers=" + mLatitude + "," + mLongitude)
                            .centerCrop()
                            .error(new TextDrawable(mLatitude + "," + mLongitude))
                            .into(imageLocationFoundDescription);

                    showComponent();
                } catch (Exception ex) {
                    Intent i = new Intent(GeolocationActivity.this, MapsActivity.class);
                    startActivityForResult(i, REQUEST_MAPS);
                }
            }
        }, 3000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MAPS) {
            if (resultCode == RESULT_OK) {
                mLatitude = Double.valueOf(data.getStringExtra("latitude"));
                mLongitude = Double.valueOf(data.getStringExtra("longitude"));
                mAddress = data.getStringExtra("address");
                onYesButtonPressed(mLatitude, mLongitude, mAddress);
                finish();
            }
        } else if (requestCode == PERMISSIONS_REQUEST_SETTINGS) {
            LocationRequest mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    private void initialComponent() {
        searchingLocationLottie = findViewById(R.id.lottie_animation_searching_location);
        searchingLocationTitle = findViewById(R.id.textView_searching_location);

        locationFoundLottie = findViewById(R.id.lottie_animation_location_found);
        locationFoundTitle = findViewById(R.id.textView_location_found_title);
        locationFoundDescription = findViewById(R.id.textView_location_found_description);
        imageLocationFoundDescription = findViewById(R.id.imageView_location_found_description);
        locationFoundQuestion = findViewById(R.id.textView_location_found_question);
        locationFoundYes = findViewById(R.id.button_location_found_yes);
        locationFoundRefresh = findViewById(R.id.button_location_found_refresh);
        locationFoundLeft = findViewById(R.id.view_location_found_left);
        locationFoundRight = findViewById(R.id.view_location_found_right);
        locationFoundOr = findViewById(R.id.textView_location_found_or);
        locationFoundGoogleMaps = findViewById(R.id.textView_location_found_google_maps);
        imageLocationFoundGoogleMaps = findViewById(R.id.image__location_found_google_maps);

        locationFoundRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupPermissions();
            }
        });

        locationFoundYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onYesButtonPressed(mLatitude, mLongitude, mAddress);
                finish();
            }
        });

        imageLocationFoundDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String geo = "geo:" + mLatitude + "," + mLongitude;
                Uri gmmIntentUri = Uri.parse(geo);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            }
        });

        locationFoundGoogleMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String geo = "geo:" + mLatitude + "," + mLongitude;
                Uri gmmIntentUri = Uri.parse(geo);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            }
        });

        imageLocationFoundGoogleMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String geo = "geo:" + mLatitude + "," + mLongitude;
                Uri gmmIntentUri = Uri.parse(geo);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            }
        });
    }

    private void showComponent() {
        searchingLocationLottie.setVisibility(View.GONE);
        searchingLocationTitle.setVisibility(View.GONE);

        locationFoundLottie.setVisibility(View.VISIBLE);
        locationFoundTitle.setVisibility(View.VISIBLE);
//        locationFoundDescription.setVisibility(View.VISIBLE);
        imageLocationFoundDescription.setVisibility(View.VISIBLE);
        locationFoundQuestion.setVisibility(View.VISIBLE);
        locationFoundYes.setVisibility(View.VISIBLE);
        locationFoundRefresh.setVisibility(View.VISIBLE);
        locationFoundLeft.setVisibility(View.VISIBLE);
        locationFoundRight.setVisibility(View.VISIBLE);
        locationFoundOr.setVisibility(View.VISIBLE);
        locationFoundGoogleMaps.setVisibility(View.VISIBLE);
        imageLocationFoundGoogleMaps.setVisibility(View.VISIBLE);
    }

    private void hideComponent() {
        searchingLocationLottie.setVisibility(View.VISIBLE);
        searchingLocationTitle.setVisibility(View.VISIBLE);

        locationFoundLottie.setVisibility(View.GONE);
        locationFoundTitle.setVisibility(View.GONE);
        locationFoundDescription.setVisibility(View.GONE);
        imageLocationFoundDescription.setVisibility(View.GONE);
        locationFoundQuestion.setVisibility(View.GONE);
        locationFoundYes.setVisibility(View.GONE);
        locationFoundRefresh.setVisibility(View.GONE);
        locationFoundLeft.setVisibility(View.GONE);
        locationFoundRight.setVisibility(View.GONE);
        locationFoundOr.setVisibility(View.GONE);
        locationFoundGoogleMaps.setVisibility(View.GONE);
        imageLocationFoundGoogleMaps.setVisibility(View.GONE);
    }

    public abstract void onYesButtonPressed(Double latitude, Double longitude, String address);

}
