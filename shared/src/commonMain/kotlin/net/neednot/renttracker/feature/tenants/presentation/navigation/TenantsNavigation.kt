package net.neednot.renttracker.feature.tenants.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import net.neednot.renttracker.core.navigation.RentTermEditorRoute
import net.neednot.renttracker.core.navigation.TenantEditorRoute
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.feature.tenants.presentation.screens.RentTermEditorScreen
import net.neednot.renttracker.feature.tenants.presentation.screens.TenantEditorScreen
import net.neednot.renttracker.feature.tenants.presentation.screens.TenantScreen

fun NavGraphBuilder.tenantsGraph(navController: NavController) {
    composable<TenantRoute> { entry ->
        val route = entry.toRoute<TenantRoute>()
        TenantScreen(
            route = route,
            onBackClicked = { navController.popBackStack() },
            onEditDetailsClicked = { groupId -> navController.navigate(TenantEditorRoute(groupId, route.tenantId)) },
            onChangeRentClicked = { navController.navigate(RentTermEditorRoute(route.tenantId)) },
            onRentTermClicked = { rentTermId ->
                navController.navigate(RentTermEditorRoute(route.tenantId, rentTermId))
            },
        )
    }
    composable<TenantEditorRoute> { entry ->
        TenantEditorScreen(
            route = entry.toRoute<TenantEditorRoute>(),
            onTenantAdded = { tenantId ->
                navController.navigate(TenantRoute(tenantId)) {
                    popUpTo<TenantEditorRoute> { inclusive = true }
                }
            },
            onClosed = { navController.popBackStack() },
        )
    }
    composable<RentTermEditorRoute> { entry ->
        RentTermEditorScreen(
            route = entry.toRoute<RentTermEditorRoute>(),
            onClosed = { navController.popBackStack() },
        )
    }
}
