package com.senjuid.location;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.content.Context;
import androidx.annotation.NonNull;

public class GeolocationViewModelFactory implements ViewModelProvider.Factory {

    private Context appContext;

    public GeolocationViewModelFactory(Context appContext) {
        this.appContext = appContext;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(GeolocationViewModel.class)) {
            return (T) new GeolocationViewModel(appContext);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
