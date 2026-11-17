package net.neednot.renttracker.feature.tenants.presentation.screens

import kotlinx.datetime.YearMonth
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorField

internal data class RentTermEditorActions(
    val onStartMonthChanged: (YearMonth) -> Unit = {},
    val onFieldChanged: (RentTermEditorField, String) -> Unit = { _, _ -> },
    val onSaveClicked: () -> Unit = {},
    val onDeleteClicked: () -> Unit = {},
    val onDeleteConfirmed: () -> Unit = {},
    val onDeleteDismissed: () -> Unit = {},
    val onCloseClicked: () -> Unit = {},
)
