package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class MemberPermissionsUiState(
    val isLoading: Boolean = true,
    val header: MemberHeaderUiState? = null,
    val form: PermissionsFormUiState? = null,
    val canRemove: Boolean = false,
    val conflictBanner: UiText? = null,
    val error: UiText? = null,
    val isSaving: Boolean = false,
    val removeDialog: RemoveMemberDialogUiState? = null,
)
