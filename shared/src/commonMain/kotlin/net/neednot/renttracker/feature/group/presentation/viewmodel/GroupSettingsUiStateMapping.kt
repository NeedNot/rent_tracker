package net.neednot.renttracker.feature.group.presentation.viewmodel

import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_settings_conflict_named
import net.neednot.renttracker.resources.group_settings_conflict_unnamed
import net.neednot.renttracker.resources.group_settings_payment_order_by_date
import net.neednot.renttracker.resources.group_settings_payment_order_rent_first

internal fun GroupDetails.toSettingsUiState(conflictTitle: UiText? = null): GroupSettingsUiState =
    if (access.canEditSettings) {
        GroupSettingsUiState.Editing(
            form = settings.toForm(),
            timeZoneOptions = timeZoneOptions(settings.timeZone),
            conflictTitle = conflictTitle,
        )
    } else {
        GroupSettingsUiState.ReadOnly(
            form = settings.toForm(),
            timeZoneOptions = timeZoneOptions(settings.timeZone),
        )
    }

internal fun PaymentOrder.label(): UiText =
    when (this) {
        PaymentOrder.RentFirst -> UiText.Resource(Res.string.group_settings_payment_order_rent_first)
        PaymentOrder.ByDate -> UiText.Resource(Res.string.group_settings_payment_order_by_date)
    }

internal fun settingsConflictTitle(editedByName: String?): UiText =
    if (editedByName == null) {
        UiText.Resource(Res.string.group_settings_conflict_unnamed)
    } else {
        UiText.Resource(Res.string.group_settings_conflict_named, listOf(editedByName))
    }
