package net.neednot.renttracker.feature.payments.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import net.neednot.renttracker.core.navigation.EditPaymentRoute
import net.neednot.renttracker.core.navigation.LogPaymentRoute
import net.neednot.renttracker.feature.payments.presentation.screens.PaymentEditorScreen
import net.neednot.renttracker.feature.payments.presentation.viewmodel.PaymentEditorTarget

fun NavGraphBuilder.paymentsGraph(navController: NavController) {
    composable<LogPaymentRoute> { entry ->
        val route = entry.toRoute<LogPaymentRoute>()
        PaymentEditorScreen(
            target = PaymentEditorTarget(tenantId = route.tenantId, paymentId = null),
            onClosed = { navController.popBackStack() },
        )
    }
    composable<EditPaymentRoute> { entry ->
        val route = entry.toRoute<EditPaymentRoute>()
        PaymentEditorScreen(
            target = PaymentEditorTarget(tenantId = route.tenantId, paymentId = route.paymentId),
            onClosed = { navController.popBackStack() },
        )
    }
}
