package com.mojgrad.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationManager private constructor(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation
    
    private val _isLocationAvailable = MutableStateFlow(false)
    val isLocationAvailable: StateFlow<Boolean> = _isLocationAvailable
    
    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted
    
    private var locationCallback: LocationCallback? = null
    private var lastLocationUpdate = 0L
    
    companion object {
        @Volatile
        private var INSTANCE: LocationManager? = null
        
        fun getInstance(context: Context): LocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationManager(context.applicationContext).also { 
                    INSTANCE = it
                    println("DEBUG: LocationManager - Creating new singleton instance")
                }
            }
        }
    }
    
    init {
        checkLocationPermissions()
    }
    
    private fun checkLocationPermissions() {
        val hasPermissions = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && 
        ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        _locationPermissionGranted.value = hasPermissions
        
        if (hasPermissions) {
            startLocationUpdates()
        }
    }
    
    fun requestPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    fun onPermissionsGranted() {
        _locationPermissionGranted.value = true
        startLocationUpdates()
    }
    
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 
            10000 // 10 sekundi interval
        ).apply {
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(false)
            setMinUpdateIntervalMillis(5000) // minimum 5 sekundi
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    
                    println("DEBUG: Location updated - ${location.latitude}, ${location.longitude}")
                    println("DEBUG: LocationManager - About to emit to ${_currentLocation.subscriptionCount.value} subscribers")
                    
                    // Eksplicitno emit na flow
                    _currentLocation.value = latLng
                    _isLocationAvailable.value = true
                    
                    println("DEBUG: LocationManager - Location emitted successfully")
                    println("DEBUG: LocationManager - currentLocation flow has ${_currentLocation.subscriptionCount.value} subscribers")
                    
                    // Ažuriraj lokaciju korisnika u Firestore svakih 30 sekundi
                    updateUserLocationInFirestore(latLng)
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )
        
        // Takođe pokušaj da dobiješ poslednju poznatu lokaciju odmah
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                _currentLocation.value = latLng
                _isLocationAvailable.value = true
                println("DEBUG: Last known location - ${it.latitude}, ${it.longitude}")
            }
        }
    }
    
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
    
    fun getCurrentLocationOnce(callback: (LatLng?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("DEBUG: LocationManager - No location permission")
            callback(null)
            return
        }
        
        println("DEBUG: LocationManager - Getting current location...")
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val latLng = location?.let { LatLng(it.latitude, it.longitude) }
            println("DEBUG: LocationManager - Got location: $latLng")
            callback(latLng)
        }.addOnFailureListener { exception ->
            println("DEBUG: LocationManager - Error getting location: ${exception.message}")
            callback(null)
        }
    }
    
    private fun updateUserLocationInFirestore(location: LatLng) {
        val currentUser = auth.currentUser ?: return
        val currentTime = System.currentTimeMillis()
        
        // Ažuriraj lokaciju samo svakih 30 sekundi da ne preopterećujemo Firestore
        if (currentTime - lastLocationUpdate < 30000) return
        
        lastLocationUpdate = currentTime
        
        val userRef = firestore.collection("users").document(currentUser.uid)
        val locationData = mapOf(
            "lastLocation" to GeoPoint(location.latitude, location.longitude),
            "lastLocationUpdate" to currentTime
        )
        
        userRef.update(locationData)
            .addOnSuccessListener {
                println("DEBUG: User location updated in Firestore: $location")
            }
            .addOnFailureListener { exception ->
                println("DEBUG: Failed to update user location in Firestore: ${exception.message}")
            }
    }
}
