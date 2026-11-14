package net.neednot.renttracker.feature.home.presentation.viewmodel

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
import net.neednot.renttracker.core.validation.InputLimits
import net.neednot.renttracker.core.validation.lengthError
import net.neednot.renttracker.feature.home.domain.HomeRepository
import net.neednot.renttracker.feature.home.presentation.uistate.CreateGroupUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.create_group_error_name_required

class CreateGroupViewModel(
    private val homeRepository: HomeRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<CreateGroupEvent>(Channel.BUFFERED)
    val events: Flow<CreateGroupEvent> = eventChannel.receiveAsFlow()

    fun onNameChanged(name: String) {
        mutableUiState.update { it.copy(name = name, error = lengthError(name, InputLimits.MAX_NAME_LENGTH)) }
    }

    fun onSaveClicked() {
        if (uiState.value.isSaving) return
        val name = uiState.value.name.trim()
        val nameError =
            if (name.isEmpty()) {
                UiText.Resource(Res.string.create_group_error_name_required)
            } else {
                lengthError(name, InputLimits.MAX_NAME_LENGTH)
            }
        if (nameError != null) {
            mutableUiState.update { it.copy(error = nameError) }
            return
        }
        save(name)
    }

    private fun save(name: String) {
        mutableUiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            homeRepository
                .createGroup(name)
                .onOk { groupId -> eventChannel.send(CreateGroupEvent.GroupCreated(groupId)) }
                .onErr { error -> mutableUiState.update { it.copy(error = error.text()) } }
            mutableUiState.update { it.copy(isSaving = false) }
        }
    }
}
