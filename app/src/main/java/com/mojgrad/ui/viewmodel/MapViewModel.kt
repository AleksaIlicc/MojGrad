package com.mojgrad.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.model.LatLng
import com.mojgrad.location.LocationManager
import kotlinx.coroutines.flow.StateFlow

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val locationManager = LocationManager.getInstance(application)


    val currentLocation: StateFlow<LatLng?> = locationManager.currentLocation
    val isLocationAvailable: StateFlow<Boolean> = locationManager.isLocationAvailable
    val locationPermissionGranted: StateFlow<Boolean> = locationManager.locationPermissionGranted

    fun requestLocationPermissions(): Array<String> {
        return locationManager.requestPermissions()
    }

    fun onLocationPermissionsGranted() {
        locationManager.onPermissionsGranted()
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.stopLocationUpdates()
    }
}
