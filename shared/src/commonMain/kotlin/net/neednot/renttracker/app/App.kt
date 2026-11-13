package net.neednot.renttracker.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.navigation.HomeRoute
import net.neednot.renttracker.core.navigation.SignInGraphRoute
import net.neednot.renttracker.feature.auth.presentation.navigation.authGraph
import net.neednot.renttracker.feature.home.presentation.navigation.homeGraph
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    RentTrackerTheme {
        val viewModel = koinViewModel<AppViewModel>()
        val sessionState by viewModel.sessionState.collectAsState()
        var startDestination by remember { mutableStateOf<Any?>(null) }
        if (startDestination == null) startDestination = sessionState.startDestination()

        when (val start = startDestination) {
            null -> SessionLoading()
            else -> RootNavHost(startDestination = start, sessionState = sessionState)
        }
    }
}

@Composable
private fun RootNavHost(
    startDestination: Any,
    sessionState: AuthSessionState,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        authGraph(navController)
        homeGraph()
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
    val description = stringResource(Res.string.auth_loading)
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
        }
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
