package com.dsmg11.nexstock.presentation.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object Home : Screen("home")
    data object Profile : Screen("profile")
    data object Users : Screen("users")

    data object Catalog : Screen("catalog")
    data object ProductForm : Screen("product_form?code={code}") {
        fun createRoute(code: String? = null) =
            if (code == null) "product_form" else "product_form?code=$code"
    }
}
