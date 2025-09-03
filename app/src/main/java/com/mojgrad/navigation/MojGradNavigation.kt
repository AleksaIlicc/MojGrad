package com.mojgrad.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mojgrad.ui.screens.AddProblemScreen
import com.mojgrad.ui.screens.LoginScreen
import com.mojgrad.ui.screens.MainScreen
import com.mojgrad.ui.screens.RegistrationScreen
import com.mojgrad.ui.viewmodel.AuthViewModel


object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val ADD_PROBLEM = "add_problem"
}

@Composable
fun MojGradNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by authViewModel.uiState.collectAsState()



    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        modifier = modifier
    ) {

        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = uiState,
                onLoginClick = { email, password ->
                    authViewModel.signIn(email, password)
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onClearError = {
                    authViewModel.clearError()
                }
            )
        }


        composable(Routes.REGISTER) {
            RegistrationScreen(
                uiState = uiState,
                onRegisterClick = { email, password, name, phone, imageUri ->
                    authViewModel.signUp(email, password, name, phone, imageUri)
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onClearError = {
                    authViewModel.clearError()
                }
            )
        }


        composable(Routes.MAIN) {
            MainScreen(
                rootNavController = navController,
                onSignOut = {
                    authViewModel.signOut()
                }
            )
        }


        composable(Routes.ADD_PROBLEM) {
            AddProblemScreen(
                onProblemAdded = {
                    navController.popBackStack()
                }
            )
        }
    }


    LaunchedEffect(uiState.isLoggedIn) {
        val currentRoute = navController.currentDestination?.route

        when {
            uiState.isLoggedIn && currentRoute != Routes.MAIN -> {
                navController.navigate(Routes.MAIN) {

                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }

                    launchSingleTop = true
                }
            }
            !uiState.isLoggedIn && currentRoute != Routes.LOGIN && currentRoute != Routes.REGISTER -> {
                navController.navigate(Routes.LOGIN) {

                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }
}
