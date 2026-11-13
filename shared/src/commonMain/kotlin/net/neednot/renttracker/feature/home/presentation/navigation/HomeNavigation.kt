package net.neednot.renttracker.feature.home.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import net.neednot.renttracker.core.navigation.CreateGroupRoute
import net.neednot.renttracker.core.navigation.GroupRoute
import net.neednot.renttracker.core.navigation.HomeRoute
import net.neednot.renttracker.feature.home.presentation.screens.CreateGroupScreen
import net.neednot.renttracker.feature.home.presentation.screens.HomeScreen

fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable<HomeRoute> {
        HomeScreen(
            onGroupClicked = { groupId -> navController.navigate(GroupRoute(groupId)) },
            onNewGroupClicked = { navController.navigate(CreateGroupRoute) },
        )
    }
    composable<CreateGroupRoute> {
        CreateGroupScreen(
            onGroupCreated = { groupId ->
                navController.navigate(GroupRoute(groupId)) {
                    popUpTo<CreateGroupRoute> { inclusive = true }
                }
            },
            onCloseClicked = { navController.popBackStack() },
        )
    }
}
