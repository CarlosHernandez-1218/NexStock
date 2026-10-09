package com.dsmg11.nexstock.presentation.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dsmg11.nexstock.domain.model.AppFeature
import com.dsmg11.nexstock.presentation.auth.ForgotPasswordScreen
import com.dsmg11.nexstock.presentation.auth.LoginScreen
import com.dsmg11.nexstock.presentation.auth.RegisterScreen
import com.dsmg11.nexstock.presentation.catalog.CatalogScreen
import com.dsmg11.nexstock.presentation.catalog.ProductFormScreen
import com.dsmg11.nexstock.presentation.home.HomeScreen
import com.dsmg11.nexstock.presentation.profile.ProfileScreen
import com.dsmg11.nexstock.presentation.users.UsersScreen
import com.dsmg11.nexstock.presentation.stock.StockScreen
import com.dsmg11.nexstock.presentation.master.MasterDataScreen
import com.dsmg11.nexstock.presentation.suppliers.SupplierListScreen
import com.dsmg11.nexstock.presentation.suppliers.SupplierScreen

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
                        AppFeature.CATALOGO -> navController.navigate(Screen.Catalog.route)
                        AppFeature.STOCK -> navController.navigate(Screen.Stock.route)
                        AppFeature.MAESTROS -> navController.navigate(Screen.MasterData.route)
                        AppFeature.PROVEEDORES -> navController.navigate(Screen.Suppliers.route)
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
        composable(Screen.Catalog.route) {
            CatalogScreen(
                onBack = { navController.popBackStack() },
                onAddProduct = { navController.navigate(Screen.ProductForm.createRoute()) },
                onEditProduct = { code -> navController.navigate(Screen.ProductForm.createRoute(code)) }
            )
        }
        composable(Screen.Stock.route) {
            StockScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.MasterData.route) {
            MasterDataScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Suppliers.route) {
            SupplierListScreen(
                onBack = { navController.popBackStack() },
                onAddSupplier = { navController.navigate(Screen.SupplierForm.route) }
            )
        }
        composable(Screen.SupplierForm.route) {
            SupplierScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.ProductForm.route,
            arguments = listOf(
                navArgument("code") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ProductFormScreen(onBack = { navController.popBackStack() })
        }
    }
}