package com.mojgrad.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@Composable
fun MainScreen(
    rootNavController: NavHostController,
    onSignOut: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
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
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> MapScreen(
                modifier = Modifier.padding(innerPadding),
                rootNavController = rootNavController
            )
            1 -> ListaScreen()
            2 -> LeaderboardScreen() // Direktno koristi LeaderboardScreen
            3 -> UserProfileScreen(onSignOut = onSignOut)
        }
    }
}
