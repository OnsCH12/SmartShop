package com.example.smartshop.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartshop.auth.LoginState
import com.example.smartshop.auth.LoginViewModel
import com.example.smartshop.products.viewmodel.ProductViewModel
import com.example.smartshop.ui.LoginScreen
import com.example.smartshop.ui.products.AddEditProductScreen
import com.example.smartshop.ui.products.ProductListScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ProductList : Screen("product_list")
    object AddProduct : Screen("add_product")
    object EditProduct : Screen("edit_product/{productId}") {
        fun createRoute(productId: String) = "edit_product/$productId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    loginViewModel: LoginViewModel = viewModel(),
    productViewModel: ProductViewModel = viewModel()
) {
    val loginState by loginViewModel.loginState.collectAsStateWithLifecycle()

    val startDestination = when (loginState) {
        is LoginState.Success -> Screen.ProductList.route
        else -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.ProductList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProductList.route) {
            ProductListScreen(
                productViewModel = productViewModel,
                loginViewModel = loginViewModel,
                onAddProduct = {
                    navController.navigate(Screen.AddProduct.route)
                },
                onEditProduct = { product ->
                    navController.navigate(Screen.EditProduct.createRoute(product.id))
                }
            )
        }

        composable(Screen.AddProduct.route) {
            AddEditProductScreen(
                viewModel = productViewModel,
                productId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            AddEditProductScreen(
                viewModel = productViewModel,
                productId = productId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}