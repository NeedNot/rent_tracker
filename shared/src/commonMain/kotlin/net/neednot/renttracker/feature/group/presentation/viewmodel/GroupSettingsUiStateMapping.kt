package net.neednot.renttracker.feature.group.presentation.viewmodel

import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_settings_conflict_named
import net.neednot.renttracker.resources.group_settings_conflict_unnamed
import net.neednot.renttracker.resources.group_settings_grace_days_value
import net.neednot.renttracker.resources.group_settings_late_fee_default
import net.neednot.renttracker.resources.group_settings_payment_order_by_date
import net.neednot.renttracker.resources.group_settings_payment_order_rent_first

internal fun GroupDetails.toSettingsUiState(conflictBanner: UiText? = null): GroupSettingsUiState =
    if (access.canEditSettings) {
        GroupSettingsUiState.Editing(
            form = settings.toForm(),
            timeZoneOptions = timeZoneOptions(settings.timeZone),
            conflictBanner = conflictBanner,
        )
    } else {
        GroupSettingsUiState.ReadOnly(
            groupName = settings.name,
            timeZone = timeZoneLabel(settings.timeZone),
            paymentOrder = settings.paymentOrder.label(),
            graceDays =
                UiText.Plural(
                    Res.plurals.group_settings_grace_days_value,
                    settings.graceDays,
                    listOf(settings.graceDays),
                ),
            lateFee =
                settings.lateFeeFlatCents?.let { UiText.Plain(formatMoney(it)) }
                    ?: UiText.Resource(Res.string.group_settings_late_fee_default),
        )
    }

internal fun PaymentOrder.label(): UiText =
    when (this) {
        PaymentOrder.RentFirst -> UiText.Resource(Res.string.group_settings_payment_order_rent_first)
        PaymentOrder.ByDate -> UiText.Resource(Res.string.group_settings_payment_order_by_date)
    }

internal fun settingsConflictBanner(editedByName: String?): UiText =
    if (editedByName == null) {
        UiText.Resource(Res.string.group_settings_conflict_unnamed)
    } else {
        UiText.Resource(
            Res.string.group_settings_conflict_named,
            listOf(editedByName, editedByName.substringBefore(' ')),
        )
    }
