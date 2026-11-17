package net.neednot.renttracker.feature.payments.presentation.screens

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.feature.payments.presentation.viewmodel.SyncAlertsAnswer

internal data class PaymentEditorActions(
    val onFieldChanged: (PaymentEditorField, String) -> Unit = { _, _ -> },
    val onDateChanged: (LocalDate) -> Unit = {},
    val onMethodClicked: (PaymentMethod) -> Unit = {},
    val onLateFeeToggled: (Boolean) -> Unit = {},
    val onSaveClicked: () -> Unit = {},
    val onDuplicateConfirmed: () -> Unit = {},
    val onDuplicateDismissed: () -> Unit = {},
    val onSyncAlertsAnswered: (SyncAlertsAnswer) -> Unit = {},
    val onCloseClicked: () -> Unit = {},
)
