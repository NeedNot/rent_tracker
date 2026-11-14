package net.neednot.renttracker.feature.debts.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.navigation.AddChargeRoute
import net.neednot.renttracker.core.navigation.EditChargeRoute
import net.neednot.renttracker.feature.debts.presentation.screens.ChargeEditorScreen
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeEditorTarget
import net.neednot.renttracker.feature.debts.presentation.viewmodel.ChargeSuggestion

fun NavGraphBuilder.debtsGraph(navController: NavController) {
    composable<AddChargeRoute> { entry ->
        val route = entry.toRoute<AddChargeRoute>()
        ChargeEditorScreen(
            target = ChargeEditorTarget(tenantId = route.tenantId, suggestion = route.toSuggestion()),
            onClosed = { navController.popBackStack() },
        )
    }
    composable<EditChargeRoute> { entry ->
        val route = entry.toRoute<EditChargeRoute>()
        ChargeEditorScreen(
            target = ChargeEditorTarget(tenantId = route.tenantId, debtId = route.debtId),
            onClosed = { navController.popBackStack() },
        )
    }
}

private fun AddChargeRoute.toSuggestion(): ChargeSuggestion? {
    val month = suggestedMonth
    val cents = suggestedCents
    return if (month != null && cents != null) ChargeSuggestion(YearMonth.parse(month), cents) else null
}
