package net.neednot.renttracker.feature.account.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import net.neednot.renttracker.core.navigation.AccountRoute
import net.neednot.renttracker.feature.account.presentation.screens.AccountScreen

fun NavGraphBuilder.accountGraph(navController: NavController) {
    composable<AccountRoute> {
        AccountScreen(onBackClicked = { navController.popBackStack() })
    }
}
