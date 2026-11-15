package net.neednot.renttracker.feature.group.data

import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.feature.group.domain.GroupSettings
import net.neednot.renttracker.model.groups.GroupsUpdateSettingsArgs
import net.neednot.renttracker.model.groups.GroupsUpdateSettingsPaymentOrder

internal fun GroupSettings.toUpdateArgs(
    groupId: String,
    expectedVersion: Long,
): GroupsUpdateSettingsArgs =
    GroupsUpdateSettingsArgs(
        expectedVersion = expectedVersion,
        graceDays = graceDays.toLong(),
        groupId = groupId,
        lateFeeFlatCents = lateFeeFlatCents,
        name = name,
        paymentOrder = paymentOrder.toConvex(),
        timeZone = timeZone,
    )

private fun PaymentOrder.toConvex(): GroupsUpdateSettingsPaymentOrder =
    when (this) {
        PaymentOrder.RentFirst -> GroupsUpdateSettingsPaymentOrder.RentFirst
        PaymentOrder.ByDate -> GroupsUpdateSettingsPaymentOrder.ByDate
    }
