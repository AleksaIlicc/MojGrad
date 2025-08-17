package com.mojgrad.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen() {
    val context = LocalContext.current
    
    // Stanje za kameru mape
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(44.787197, 20.457273), 10f) // Beograd
    }

    // Debug info
    LaunchedEffect(Unit) {
        println("DEBUG: MapScreen loaded successfully")
    }

    // Google Maps
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false // Iskljućeno za sada da ne traži dozvole
        )
    ) {
        // Ovde ćemo kasnije dodavati markere za probleme
    }
}
