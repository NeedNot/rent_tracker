package net.neednot.renttracker.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.domain.AuthRepository
import net.neednot.renttracker.feature.auth.presentation.uistate.SignInUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_error_invalid_email

class SignInViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events: Flow<SignInEvent> = eventChannel.receiveAsFlow()

    fun onEmailChanged(email: String) {
        mutableUiState.update { it.copy(email = email, emailError = null, error = null) }
    }

    fun onSendCodeClicked() {
        val state = uiState.value
        if (state.isBusy) return
        val email = state.email.trim()
        if (!EMAIL_SHAPE.matches(email)) {
            mutableUiState.update { it.copy(emailError = UiText.Resource(Res.string.auth_error_invalid_email)) }
            return
        }
        mutableUiState.update { it.copy(isSendingCode = true, emailError = null, error = null) }
        viewModelScope.launch {
            authRepository
                .sendSignInCode(email)
                .onOk { eventChannel.send(SignInEvent.CodeSent(email)) }
                .onErr { error -> mutableUiState.update { it.copy(error = error.whileSendingCode()) } }
            mutableUiState.update { it.copy(isSendingCode = false) }
        }
    }

    fun onGoogleSignInClicked() {
        if (uiState.value.isBusy) return
        mutableUiState.update { it.copy(isSigningInWithGoogle = true, emailError = null, error = null) }
        viewModelScope.launch {
            authRepository
                .signInWithGoogle()
                .onOk { account -> eventChannel.send(SignInEvent.SignedIn(account.destination())) }
                .onErr { error -> mutableUiState.update { it.copy(error = error.commonText()) } }
            mutableUiState.update { it.copy(isSigningInWithGoogle = false) }
        }
    }

    private companion object {
        val EMAIL_SHAPE = Regex("""[^@\s]+@[^@\s]+\.[^@\s]+""")
    }
}
