package net.neednot.renttracker.feature.debts.presentation.screens

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorField

internal data class ChargeEditorActions(
    val onKindSelected: (DebtKind) -> Unit = {},
    val onFieldChanged: (ChargeEditorField, String) -> Unit = { _, _ -> },
    val onDateChanged: (LocalDate) -> Unit = {},
    val onForMonthChanged: (YearMonth) -> Unit = {},
    val onSaveClicked: () -> Unit = {},
    val onDuplicateConfirmed: () -> Unit = {},
    val onDuplicateDismissed: () -> Unit = {},
    val onCloseClicked: () -> Unit = {},
)
