package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface GroupSettingsUiState {
    data object Loading : GroupSettingsUiState

    data class Error(
        val message: UiText,
    ) : GroupSettingsUiState

    data class ReadOnly(
        val groupName: String,
        val timeZone: UiText,
        val paymentOrder: UiText,
        val graceDays: UiText,
        val lateFee: UiText,
    ) : GroupSettingsUiState

    data class Editing(
        val form: GroupSettingsForm,
        val timeZoneOptions: List<TimeZoneOptionUiState>,
        val fieldErrors: Map<GroupSettingsField, UiText> = emptyMap(),
        val conflictBanner: UiText? = null,
        val error: UiText? = null,
        val isSaving: Boolean = false,
    ) : GroupSettingsUiState
}
