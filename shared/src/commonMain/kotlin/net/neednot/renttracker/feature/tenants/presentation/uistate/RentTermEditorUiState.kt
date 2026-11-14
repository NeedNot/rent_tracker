package net.neednot.renttracker.feature.tenants.presentation.uistate

import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.ui.UiText

data class RentTermEditorUiState(
    val isNewTerm: Boolean,
    val isLoading: Boolean = true,
    val startMonth: YearMonth? = null,
    val amount: String = "",
    val dueDay: String = "",
    val fieldErrors: Map<RentTermEditorField, UiText> = emptyMap(),
    val pastMonthWarning: PastMonthWarningUiState? = null,
    val showsDueDayReminder: Boolean = false,
    val canDelete: Boolean = false,
    val deletion: RentTermDeletionUiState? = null,
    val error: UiText? = null,
    val isSaving: Boolean = false,
)
