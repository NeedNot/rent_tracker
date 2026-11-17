package net.neednot.renttracker.feature.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.GroupRepository
import net.neednot.renttracker.feature.group.domain.GroupSettings
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsField
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsForm
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsUiState

class GroupSettingsViewModel(
    private val groupId: String,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<GroupSettingsUiState>(GroupSettingsUiState.Loading)
    val uiState: StateFlow<GroupSettingsUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<GroupSettingsEvent>(Channel.BUFFERED)
    val events: Flow<GroupSettingsEvent> = eventChannel.receiveAsFlow()

    private var expectedVersion: Long? = null
    private var hasTriedSaving = false

    init {
        viewModelScope.launch {
            groupRepository.observeGroup(groupId).collect { snapshot ->
                if (mutableUiState.value !is GroupSettingsUiState.Editing) show(snapshot)
            }
        }
    }

    fun onFieldChanged(
        field: GroupSettingsField,
        text: String,
    ) {
        updateForm { it.with(field, text) }
    }

    fun onTimeZoneSelected(zoneId: String) {
        updateForm { it.copy(timeZone = zoneId) }
    }

    fun onPaymentOrderSelected(order: PaymentOrder) {
        updateForm { it.copy(paymentOrder = order) }
    }

    fun onSaveClicked() {
        val state = mutableUiState.value as? GroupSettingsUiState.Editing ?: return
        val version = expectedVersion
        if (state.isSaving || version == null) return
        hasTriedSaving = true
        state.form
            .validate()
            .onOk { settings -> save(version, settings) }
            .onErr { fieldErrors -> updateEditing { it.copy(fieldErrors = fieldErrors) } }
    }

    private fun save(
        version: Long,
        settings: GroupSettings,
    ) {
        updateEditing { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            groupRepository
                .updateSettings(groupId, version, settings)
                .onOk { eventChannel.send(GroupSettingsEvent.Saved) }
                .onErr { error -> onSaveFailed(error) }
            updateEditing { it.copy(isSaving = false) }
        }
    }

    private suspend fun onSaveFailed(error: GroupError) {
        if (error is GroupError.EditConflict) {
            show(groupRepository.groupFromServer(groupId), settingsConflictTitle(error.editedByName))
            return
        }
        val field = (error as? GroupError.ValidationFailed)?.let { groupSettingsFieldOf(it.field) }
        updateEditing {
            if (field != null) {
                it.copy(fieldErrors = it.fieldErrors + (field to error.text()))
            } else {
                it.copy(error = error.text())
            }
        }
    }

    private fun updateForm(change: (GroupSettingsForm) -> GroupSettingsForm) {
        updateEditing {
            val form = change(it.form)
            it.copy(form = form, fieldErrors = form.fieldErrors(includesMissing = hasTriedSaving), error = null)
        }
    }

    private fun updateEditing(change: (GroupSettingsUiState.Editing) -> GroupSettingsUiState.Editing) {
        mutableUiState.update { if (it is GroupSettingsUiState.Editing) change(it) else it }
    }

    private fun show(
        snapshot: Synced<GroupDetails?, GroupError>,
        conflictTitle: UiText? = null,
    ) {
        val group = snapshot.value
        expectedVersion = group?.version
        hasTriedSaving = false
        mutableUiState.value =
            when {
                group != null -> group.toSettingsUiState(conflictTitle)
                snapshot.status is SyncStatus.Failed -> GroupSettingsUiState.Error(snapshot.status.error.text())
                else -> GroupSettingsUiState.Loading
            }
    }
}

private suspend fun GroupRepository.groupFromServer(groupId: String): Synced<GroupDetails?, GroupError> =
    observeGroup(groupId).first { it.status != SyncStatus.Syncing }
