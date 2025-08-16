package com.mojgrad.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mojgrad.ui.screens.HomeScreen
import com.mojgrad.ui.screens.LoginScreen
import com.mojgrad.ui.screens.RegistrationScreen

// Konstante za rute
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
}

@Composable
fun MojGradNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.LOGIN,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Login ekran
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginClick = { email, password ->
                    // TODO: Implementirati logiku za prijavu u ViewModelu
                    // Za sada samo navigiramo na home
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        // Registration ekran
        composable(Routes.REGISTER) {
            RegistrationScreen(
                onRegisterClick = { email, password, name, phone, imageUri ->
                    // TODO: Implementirati logiku za registraciju u ViewModelu
                    // Za sada samo navigiramo na home
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Home ekran
        composable(Routes.HOME) {
            HomeScreen(
                onLogoutClick = {
                    // TODO: Implementirati logiku za odjavu u ViewModelu
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
