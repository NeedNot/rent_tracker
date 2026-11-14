package net.neednot.renttracker.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.domain.AuthError
import net.neednot.renttracker.feature.auth.domain.AuthRepository
import net.neednot.renttracker.feature.auth.presentation.uistate.SignInCodeUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_error_code_incomplete
import kotlin.time.Duration.Companion.seconds

class SignInCodeViewModel(
    private val email: String,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SignInCodeUiState(email = email))
    val uiState: StateFlow<SignInCodeUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<SignInCodeEvent>(Channel.BUFFERED)
    val events: Flow<SignInCodeEvent> = eventChannel.receiveAsFlow()

    private var countdown: Job? = null

    init {
        startCountdown(NEW_CODE_WAIT_SECONDS)
    }

    fun onVerifyClicked(code: String) {
        if (uiState.value.isBusy) return
        if (code.length != CODE_LENGTH || !code.all(Char::isDigit)) {
            mutableUiState.update { it.copy(error = UiText.Resource(Res.string.auth_error_code_incomplete)) }
            return
        }
        mutableUiState.update { it.copy(isVerifying = true, error = null) }
        viewModelScope.launch {
            authRepository
                .signInWithCode(email, code)
                .onOk { account -> eventChannel.send(SignInCodeEvent.SignedIn(account.destination())) }
                .onErr { error -> mutableUiState.update { it.copy(error = error.whileCheckingCode()) } }
            mutableUiState.update { it.copy(isVerifying = false) }
        }
    }

    fun onSendNewCodeClicked() {
        if (!uiState.value.canSendNewCode) return
        mutableUiState.update { it.copy(isSendingNewCode = true, error = null) }
        viewModelScope.launch {
            authRepository
                .sendSignInCode(email)
                .onOk { startCountdown(NEW_CODE_WAIT_SECONDS) }
                .onErr { error ->
                    mutableUiState.update { it.copy(error = error.whileSendingCode()) }
                    if (error is AuthError.RateLimited) startCountdown(error.retryAfterSeconds ?: NEW_CODE_WAIT_SECONDS)
                }
            mutableUiState.update { it.copy(isSendingNewCode = false) }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        mutableUiState.update { it.copy(secondsUntilNewCode = seconds) }
        countdown =
            viewModelScope.launch {
                while (uiState.value.secondsUntilNewCode > 0) {
                    delay(1.seconds)
                    mutableUiState.update { it.copy(secondsUntilNewCode = it.secondsUntilNewCode - 1) }
                }
            }
    }

    private companion object {
        const val CODE_LENGTH = 6
        const val NEW_CODE_WAIT_SECONDS = 60
    }
}
