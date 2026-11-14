package net.neednot.renttracker.feature.payments.data

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.GroupEntity

internal data class GroupToday(
    val group: GroupEntity,
    val today: LocalDate,
)
