package com.dsmg11.nexstock.presentation.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dsmg11.nexstock.presentation.auth.ForgotPasswordScreen
import com.dsmg11.nexstock.presentation.auth.LoginScreen
import com.dsmg11.nexstock.presentation.auth.RegisterScreen
import com.dsmg11.nexstock.presentation.home.HomeScreen
import com.dsmg11.nexstock.presentation.profile.ProfileScreen
import androidx.compose.ui.platform.LocalContext
import com.dsmg11.nexstock.domain.model.AppFeature
import com.dsmg11.nexstock.presentation.users.UsersScreen

@Composable
fun NexStockNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = sessionViewModel.startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate(Screen.Register.route) },
                onGoToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Home.route) {
            val context = LocalContext.current
            HomeScreen(
                onGoToProfile = { navController.navigate(Screen.Profile.route) },
                onOpenFeature = { feature ->
                    when (feature) {
                        AppFeature.USUARIOS -> navController.navigate(Screen.Users.route)
                        else -> Toast.makeText(
                            context,
                            "${feature.title}: disponible en el Sprint ${feature.sprint}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Users.route) {
            UsersScreen(onBack = { navController.popBackStack() })
        }
    }
}