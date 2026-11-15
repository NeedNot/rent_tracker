package net.neednot.renttracker.feature.group.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.format.formatDollarsInput
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.feature.group.domain.GroupSettings
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsField
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsForm

internal fun GroupSettings.toForm(): GroupSettingsForm =
    GroupSettingsForm(
        name = name,
        timeZone = timeZone,
        paymentOrder = paymentOrder,
        graceDays = graceDays.toString(),
        lateFee = lateFeeFlatCents?.let(::formatDollarsInput).orEmpty(),
    )

internal fun GroupSettingsForm.with(
    field: GroupSettingsField,
    text: String,
): GroupSettingsForm =
    when (field) {
        GroupSettingsField.Name -> copy(name = text)
        GroupSettingsField.GraceDays -> copy(graceDays = text)
        GroupSettingsField.LateFee -> copy(lateFee = text)
    }

internal fun GroupSettingsForm.toSettings(): GroupSettings =
    GroupSettings(
        name = name.trim(),
        timeZone = timeZone,
        paymentOrder = paymentOrder,
        graceDays = graceDays.trim().toInt(),
        lateFeeFlatCents = parseDollarsToCents(lateFee),
    )
