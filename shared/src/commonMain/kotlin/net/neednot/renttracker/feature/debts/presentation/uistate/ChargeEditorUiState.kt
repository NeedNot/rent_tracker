package net.neednot.renttracker.feature.debts.presentation.uistate

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.ui.UiText

data class ChargeEditorUiState(
    val isNewCharge: Boolean,
    val isLoading: Boolean = true,
    val header: ChargeHeaderUiState? = null,
    val today: LocalDate? = null,
    val kind: DebtKind = DebtKind.LateFee,
    val amount: String = "",
    val date: LocalDate? = null,
    val forMonth: YearMonth? = null,
    val note: String = "",
    val isFromSuggestion: Boolean = false,
    val fieldErrors: Map<ChargeEditorField, UiText> = emptyMap(),
    val conflictBanner: UiText? = null,
    val error: UiText? = null,
    val duplicate: LateFeeDuplicateUiState? = null,
    val isSaving: Boolean = false,
)
