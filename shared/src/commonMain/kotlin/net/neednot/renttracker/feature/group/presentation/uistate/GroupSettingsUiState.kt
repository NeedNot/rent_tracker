package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface GroupSettingsUiState {
    data object Loading : GroupSettingsUiState

    data class Error(
        val message: UiText,
    ) : GroupSettingsUiState

    data class ReadOnly(
        val form: GroupSettingsForm,
        val timeZoneOptions: List<TimeZoneOptionUiState>,
    ) : GroupSettingsUiState

    data class Editing(
        val form: GroupSettingsForm,
        val timeZoneOptions: List<TimeZoneOptionUiState>,
        val fieldErrors: Map<GroupSettingsField, UiText> = emptyMap(),
        val conflictTitle: UiText? = null,
        val error: UiText? = null,
        val isSaving: Boolean = false,
    ) : GroupSettingsUiState
}
