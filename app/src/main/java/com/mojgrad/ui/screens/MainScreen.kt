package com.mojgrad.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.LatLng
import com.mojgrad.ui.viewmodel.AuthViewModel
import com.mojgrad.ui.viewmodel.ListViewModel
import com.mojgrad.ui.viewmodel.MapViewModel

@Composable
fun MainScreen(
    rootNavController: NavHostController,
    onSignOut: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var targetLocation by remember { mutableStateOf<LatLng?>(null) }


    val listViewModel: ListViewModel = viewModel()
    val mapViewModel: MapViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()


    val authUiState by authViewModel.uiState.collectAsState()
    val currentUser = authUiState.currentUser


    LaunchedEffect(currentUser) {
        println("DEBUG: MainScreen - CurrentUser: ${currentUser?.name}, admin: ${currentUser?.admin}")
    }


    val currentLocation by mapViewModel.currentLocation.collectAsState()
    LaunchedEffect(currentLocation) {
        println("DEBUG: MainScreen - Location changed: $currentLocation")
        listViewModel.updateUserLocation(
            currentLocation?.latitude,
            currentLocation?.longitude
        )
    }


    if (currentUser?.admin == true) {

        AdminScreen(
            listViewModel = listViewModel,
            onSignOut = onSignOut
        )
    } else {

        val tabs = listOf(
            Triple("Mapa", Icons.Default.LocationOn, "mapa"),
            Triple("Lista", Icons.Default.List, "lista"),
            Triple("Rang Lista", Icons.Default.Star, "rang"),
            Triple("Profil", Icons.Default.Person, "profil")
        )

        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, (title, icon, _) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = title) },
                            label = { Text(title) },
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index

                                if (index == 0) {
                                    targetLocation = null
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (selectedTabIndex) {
                0 -> MapScreen(
                    modifier = Modifier.padding(innerPadding),
                    mapViewModel = mapViewModel,
                    listViewModel = listViewModel,
                    rootNavController = rootNavController,
                    targetLocation = targetLocation
                )
                1 -> ListScreen(
                    modifier = Modifier.padding(innerPadding),
                    viewModel = listViewModel,
                    currentUser = currentUser,
                    onMapClick = { problem ->

                        problem.location?.let { geoPoint ->
                            targetLocation = LatLng(geoPoint.latitude, geoPoint.longitude)
                            selectedTabIndex = 0
                        }
                    }
                )
                2 -> LeaderboardScreen()
                3 -> UserProfileScreen(
                    onSignOut = onSignOut
                )
            }
        }
    }
}