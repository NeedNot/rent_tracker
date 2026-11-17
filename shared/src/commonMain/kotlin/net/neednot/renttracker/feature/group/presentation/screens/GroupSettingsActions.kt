package net.neednot.renttracker.feature.group.presentation.screens

import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsField

internal data class GroupSettingsActions(
    val onFieldChanged: (GroupSettingsField, String) -> Unit = { _, _ -> },
    val onTimeZoneSelected: (String) -> Unit = {},
    val onPaymentOrderSelected: (PaymentOrder) -> Unit = {},
    val onSaveClicked: () -> Unit = {},
    val onClosed: () -> Unit = {},
)
