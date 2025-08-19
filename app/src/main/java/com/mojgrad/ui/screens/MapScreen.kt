package com.mojgrad.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mojgrad.navigation.Routes
import com.mojgrad.ui.viewmodel.MapViewModel
import com.mojgrad.ui.viewmodel.ListViewModel

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel(),
    listViewModel: ListViewModel = viewModel(), // Dodano za filtriranje
    rootNavController: NavHostController,
    targetLocation: LatLng? = null // Nova lokacija na koju treba da se fokusira
) {
    // Koristimo filtrirane probleme iz ListViewModel umesto mapViewModel
    val problems by listViewModel.problems.collectAsState()
    val searchQuery by listViewModel.searchQuery.collectAsState()

    val currentLocation by mapViewModel.currentLocation.collectAsState()
    val isLocationAvailable by mapViewModel.isLocationAvailable.collectAsState()
    val locationPermissionGranted by mapViewModel.locationPermissionGranted.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            mapViewModel.onLocationPermissionsGranted()
            showPermissionDialog = false
        } else {
            // Ako permissions nisu odobreni, ostavi dialog otvoren
            println("DEBUG: Location permissions denied")
        }
    }

    // Stanje za kameru mape - koristi target lokaciju ili korisničku lokaciju ako je dostupna
    val initialLocation = targetLocation ?: currentLocation ?: LatLng(
        44.787197,
        20.457273
    ) // Target > User location > Beograd fallback
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }

    // Ažuriranje kamere kada se lokacija promeni (samo ako nema target lokacije)
    LaunchedEffect(currentLocation) {
        if (targetLocation == null) {
            currentLocation?.let { location ->
                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                println("DEBUG: Camera moved to user location: $location")
            }
        }
    }

    // Fokusiranje na target lokaciju (ima prioritet nad korisničkom lokacijom)
    LaunchedEffect(targetLocation) {
        targetLocation?.let { location ->
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(location, 16f) // Malo veći zoom za target
            println("DEBUG: Camera focused on target location: $location")
        }
    }

    // Zahtevaj permissions kada se component učita (samo jednom)
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            showPermissionDialog = true
        }
    }

    // Debug info - ažurirano da prati promene
    LaunchedEffect(problems) {
        println("DEBUG: MapScreen problems updated - showing ${problems.size} problems")
        problems.forEach { problem ->
            println("DEBUG: Marker - ${problem.category}: ${problem.description} at ${problem.location}")
        }
    }

    // Debug search query changes
    LaunchedEffect(searchQuery) {
        println("DEBUG: MapScreen search query changed to: '$searchQuery'")
    }

    // Box koji sadrži search bar, mapu i FloatingActionButton
    Column(modifier = modifier.fillMaxSize()) {
        // Search Bar i Filter Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { listViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Pretraži probleme...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { listViewModel.updateSearchQuery("") }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Filter Button
                IconButton(
                    onClick = { showFilterDialog = true }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Filter")
                }
            }
        }

        // Mapa
        Box(modifier = Modifier.fillMaxSize()) {
            // Google Maps sa markerima i user location
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionGranted && isLocationAvailable
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = true
                )
            ) {
                // Debug log za marker recomposition
                println("DEBUG: GoogleMap recomposing with ${problems.size} markers")
                
                // Iteriraj kroz listu problema i dodaj marker za svaki
                problems.forEach { problem ->
                    problem.location?.let { geoPoint ->
                        println("DEBUG: Creating marker for ${problem.category} at ${geoPoint.latitude}, ${geoPoint.longitude}")
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    geoPoint.latitude,
                                    geoPoint.longitude
                                )
                            ),
                            title = problem.category,
                            snippet = problem.description
                        )
                    }
                }
            }

            // FloatingActionButton za dodavanje problema - prikazuje se samo ako su dozvole odobrene
            if (locationPermissionGranted) {
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

            // Poruka o potrebi za lokacijom - prikazuje se samo ako dozvole nisu odobrene
            if (!locationPermissionGranted) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .fillMaxWidth(0.8f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Lokacija potrebna",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Da biste prijavili probleme, dozvolite pristup lokaciji.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val permissions = mapViewModel.requestLocationPermissions()
                                permissionLauncher.launch(permissions)
                            }
                        ) {
                            Text(
                                text = "Aktiviraj lokaciju",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Filter Dialog - koristi postojeći FilterDialog sa showSorting = false
        if (showFilterDialog) {
            com.mojgrad.ui.screens.FilterDialog(
                viewModel = listViewModel,
                onDismiss = { showFilterDialog = false },
                showSorting = false
            )
        }

        // Permission dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Potrebna dozvola za lokaciju") },
                text = {
                    Text("Aplikaciji je potrebna dozvola za pristup lokaciji da bi prikazala vašu trenutnu poziciju na mapi i omogućila Vam da prijavite probleme na tačnom mestu.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPermissionDialog = false
                            val permissions = mapViewModel.requestLocationPermissions()
                            permissionLauncher.launch(permissions)
                        }
                    ) {
                        Text("Dozvoli")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPermissionDialog = false }
                    ) {
                        Text("Ne sada")
                    }
                }
            )
        }
    }
}
