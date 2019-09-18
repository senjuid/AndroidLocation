package com.senjuid.location;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;

public class GDLocationHelper {
    private Activity activity;
    private GDLocationHelperListener listener;

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Location location = intent.getParcelableExtra(GDLocationService.INTENT_LOCATION_VALUE);
                if(location != null) {
                    listener.onLocationSuccess(location);
                    return;
                }

                String error = intent.getParcelableExtra(GDLocationService.INTENT_LOCATION_ERROR);
                listener.onLocationError(error);
                return;
            }

            listener.onLocationError("Error undefined");
        }
    };

    public GDLocationHelper(Activity activity, GDLocationHelperListener listener){
        this.activity = activity;
        this.listener = listener;
    }

    public void start(){
        activity.registerReceiver(locationReceiver, new IntentFilter(GDLocationService.MY_LOCATION));
    }

    public void stop(){
        activity.unregisterReceiver(locationReceiver);
    }

    public void requestLocation(){
        // Starting service
        Intent intent = new Intent(activity, GDLocationService.class);
        activity.startService(intent);
    }

    public interface GDLocationHelperListener {
        void onLocationSuccess(Location location);
        void onLocationError(String error);
    }
}
