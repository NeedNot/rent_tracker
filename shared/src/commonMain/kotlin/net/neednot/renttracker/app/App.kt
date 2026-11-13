package net.neednot.renttracker.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.navigation.HomeRoute
import net.neednot.renttracker.feature.group.presentation.navigation.groupGraph
import net.neednot.renttracker.feature.home.presentation.navigation.homeGraph
import net.neednot.renttracker.feature.tenants.presentation.navigation.tenantsGraph

@Composable
fun App() {
    RentTrackerTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = HomeRoute) {
            homeGraph(navController)
            groupGraph(navController)
            tenantsGraph(navController)
        }
    }
}
