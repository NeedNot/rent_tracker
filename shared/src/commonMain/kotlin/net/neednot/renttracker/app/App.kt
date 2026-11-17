package net.neednot.renttracker.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.navigation.HomeRoute
import net.neednot.renttracker.core.navigation.SignInGraphRoute
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.core.ui.AppSnackbarHost
import net.neednot.renttracker.feature.account.presentation.navigation.accountGraph
import net.neednot.renttracker.feature.auth.presentation.navigation.authGraph
import net.neednot.renttracker.feature.debts.presentation.navigation.debtsGraph
import net.neednot.renttracker.feature.group.presentation.navigation.groupGraph
import net.neednot.renttracker.feature.home.presentation.navigation.homeGraph
import net.neednot.renttracker.feature.members.presentation.navigation.membersGraph
import net.neednot.renttracker.feature.payments.presentation.navigation.paymentsGraph
import net.neednot.renttracker.feature.tenants.presentation.navigation.tenantsGraph
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    RentTrackerTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val sessionState by viewModel.sessionState.collectAsState()
        var startDestination by remember { mutableStateOf<Any?>(null) }
        if (startDestination == null) startDestination = sessionState.startDestination()

        AppSnackbarHost {
            when (val start = startDestination) {
                null -> SessionLoading()
                else -> RootNavHost(startDestination = start, sessionState = sessionState, events = viewModel.events)
            }
        }
    }
}

@Composable
private fun RootNavHost(
    startDestination: Any,
    sessionState: AuthSessionState,
    events: Flow<AppEvent>,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        authGraph(navController)
        homeGraph(navController)
        groupGraph(navController)
        tenantsGraph(navController)
        paymentsGraph(navController)
        debtsGraph(navController)
        membersGraph(navController)
        accountGraph(navController)
    }
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                AppEvent.ReturnHome -> {
                    navController.popBackStack<HomeRoute>(inclusive = false)
                }

                is AppEvent.OpenPendingPayment -> {
                    navController.navigate(TenantRoute(event.tenantId, pendingClientId = event.clientId)) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }
    LaunchedEffect(sessionState) {
        if (sessionState == AuthSessionState.SignedOut && !navController.isSigningIn()) {
            navController.navigate(SignInGraphRoute) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }
}

@Composable
private fun SessionLoading() {
    Surface(modifier = Modifier.fillMaxSize()) {
        LoadingIndicator()
    }
}

private fun AuthSessionState.startDestination(): Any? =
    when (this) {
        AuthSessionState.Loading -> null
        AuthSessionState.SignedOut -> SignInGraphRoute
        is AuthSessionState.SignedIn -> HomeRoute
    }

private fun NavController.isSigningIn(): Boolean =
    currentDestination?.hierarchy?.any { it.hasRoute<SignInGraphRoute>() } == true
