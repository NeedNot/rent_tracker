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
import net.neednot.renttracker.feature.auth.presentation.uistate.DisplayNameUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_error_display_name_length

class DisplayNameViewModel(
    private val defaultDisplayName: String,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DisplayNameUiState(displayName = defaultDisplayName))
    val uiState: StateFlow<DisplayNameUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<DisplayNameEvent>(Channel.BUFFERED)
    val events: Flow<DisplayNameEvent> = eventChannel.receiveAsFlow()

    fun onDisplayNameChanged(displayName: String) {
        mutableUiState.update { it.copy(displayName = displayName, error = null) }
    }

    fun onContinueClicked() {
        if (uiState.value.isSaving) return
        val displayName = uiState.value.displayName.trim()
        when {
            displayName.length !in 1..MAX_LENGTH -> {
                mutableUiState.update { it.copy(error = UiText.Resource(Res.string.auth_error_display_name_length)) }
            }

            displayName == defaultDisplayName -> {
                eventChannel.trySend(DisplayNameEvent.Done)
            }

            else -> {
                save(displayName)
            }
        }
    }

    private fun save(displayName: String) {
        mutableUiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            authRepository
                .setDisplayName(displayName)
                .onOk { eventChannel.send(DisplayNameEvent.Done) }
                .onErr { error -> mutableUiState.update { it.copy(error = error.commonText()) } }
            mutableUiState.update { it.copy(isSaving = false) }
        }
    }

    private companion object {
        const val MAX_LENGTH = 100
    }
}
