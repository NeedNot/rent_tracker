package net.neednot.renttracker.feature.home.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import net.neednot.renttracker.core.navigation.HomeRoute
import net.neednot.renttracker.feature.home.presentation.screens.HomeScreen

fun NavGraphBuilder.homeGraph() {
    composable<HomeRoute> {
        HomeScreen()
    }
}
