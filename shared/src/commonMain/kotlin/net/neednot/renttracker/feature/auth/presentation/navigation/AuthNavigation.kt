package net.neednot.renttracker.feature.auth.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import net.neednot.renttracker.core.navigation.DisplayNameRoute
import net.neednot.renttracker.core.navigation.HomeRoute
import net.neednot.renttracker.core.navigation.SignInCodeRoute
import net.neednot.renttracker.core.navigation.SignInGraphRoute
import net.neednot.renttracker.core.navigation.SignInRoute
import net.neednot.renttracker.feature.auth.presentation.screens.DisplayNameScreen
import net.neednot.renttracker.feature.auth.presentation.screens.SignInCodeScreen
import net.neednot.renttracker.feature.auth.presentation.screens.SignInScreen
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignedInDestination

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation<SignInGraphRoute>(startDestination = SignInRoute) {
        composable<SignInRoute> {
            SignInScreen(
                onCodeSent = { email -> navController.navigate(SignInCodeRoute(email)) },
                onSignedIn = navController::leaveSignIn,
            )
        }
        composable<SignInCodeRoute> { entry ->
            SignInCodeScreen(
                email = entry.toRoute<SignInCodeRoute>().email,
                onSignedIn = navController::leaveSignIn,
                onBackClicked = { navController.popBackStack() },
            )
        }
    }
    composable<DisplayNameRoute> { entry ->
        DisplayNameScreen(
            defaultDisplayName = entry.toRoute<DisplayNameRoute>().defaultDisplayName,
            onDone = {
                navController.navigate(HomeRoute) {
                    popUpTo<DisplayNameRoute> { inclusive = true }
                }
            },
        )
    }
}

private fun NavController.leaveSignIn(destination: SignedInDestination) {
    val route: Any =
        when (destination) {
            SignedInDestination.Home -> HomeRoute
            is SignedInDestination.DisplayName -> DisplayNameRoute(destination.defaultDisplayName)
        }
    navigate(route) {
        popUpTo<SignInGraphRoute> { inclusive = true }
    }
}
