package com.mojgrad.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mojgrad.navigation.Routes
import com.mojgrad.ui.viewmodel.MapViewModel

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel(),
    rootNavController: NavHostController
) {
    val problems by mapViewModel.problems.collectAsState()
    
    // Stanje za kameru mape
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(44.787197, 20.457273), 12f) // Beograd sa zoom 12
    }

    // Debug info
    LaunchedEffect(Unit) {
        println("DEBUG: MapScreen started with ViewModel")
    }

    LaunchedEffect(problems) {
        println("DEBUG: MapScreen received ${problems.size} problems")
        problems.forEach { problem ->
            println("DEBUG: Problem - ${problem.category}: ${problem.description} at ${problem.location}")
        }
    }

    // Box koji sadrži mapu i FloatingActionButton
    Box(modifier = modifier.fillMaxSize()) {
        // Google Maps sa markerima
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false // Sigurno false da izbegne permission crash
            )
        ) {
            // Iteriraj kroz listu problema i dodaj marker za svaki
            problems.forEach { problem ->
                problem.location?.let { geoPoint ->
                    Marker(
                        state = MarkerState(position = LatLng(geoPoint.latitude, geoPoint.longitude)),
                        title = problem.category,
                        snippet = problem.description
                    )
                }
            }
        }
        
        // FloatingActionButton za dodavanje problema
        FloatingActionButton(
            onClick = { 
                rootNavController.navigate(Routes.ADD_PROBLEM) 
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Prijavi Problem")
        }
    }
}
