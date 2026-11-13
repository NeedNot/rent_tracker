package net.neednot.renttracker.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.feature.auth.domain.AuthRepository

class AppViewModel(
    authRepository: AuthRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
}
