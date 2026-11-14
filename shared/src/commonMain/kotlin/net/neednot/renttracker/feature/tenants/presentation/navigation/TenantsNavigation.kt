package net.neednot.renttracker.feature.tenants.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import net.neednot.renttracker.core.navigation.AddChargeRoute
import net.neednot.renttracker.core.navigation.EditChargeRoute
import net.neednot.renttracker.core.navigation.EditPaymentRoute
import net.neednot.renttracker.core.navigation.LedgerRoute
import net.neednot.renttracker.core.navigation.LogPaymentRoute
import net.neednot.renttracker.core.navigation.RentTermEditorRoute
import net.neednot.renttracker.core.navigation.TenantEditorRoute
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.feature.tenants.presentation.screens.LedgerScreen
import net.neednot.renttracker.feature.tenants.presentation.screens.RentTermEditorScreen
import net.neednot.renttracker.feature.tenants.presentation.screens.TenantEditorScreen
import net.neednot.renttracker.feature.tenants.presentation.screens.TenantScreen
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryEditTarget

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
            onLogPaymentClicked = { navController.navigate(LogPaymentRoute(route.tenantId)) },
            onAddChargeClicked = { suggestedMonth, suggestedCents ->
                navController.navigate(AddChargeRoute(route.tenantId, suggestedMonth, suggestedCents))
            },
            onViewLedgerClicked = { navController.navigate(LedgerRoute(route.tenantId)) },
            onEditEntryClicked = { target -> navController.navigateToEdit(route.tenantId, target) },
        )
    }
    composable<LedgerRoute> { entry ->
        val route = entry.toRoute<LedgerRoute>()
        LedgerScreen(
            route = route,
            onBackClicked = { navController.popBackStack() },
            onLogPaymentClicked = { navController.navigate(LogPaymentRoute(route.tenantId)) },
            onEditEntryClicked = { target -> navController.navigateToEdit(route.tenantId, target) },
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

private fun NavController.navigateToEdit(
    tenantId: String,
    target: EntryEditTarget,
) {
    when (target) {
        is EntryEditTarget.Payment -> navigate(EditPaymentRoute(tenantId, target.paymentId))
        is EntryEditTarget.Charge -> navigate(EditChargeRoute(tenantId, target.debtId))
    }
}
