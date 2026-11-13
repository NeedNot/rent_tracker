package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.YearMonth

internal data class LoadedRentTerm(
    val currentMonth: YearMonth,
    val startMonth: YearMonth,
    val amount: String,
    val dueDay: Int,
    val expectedVersion: Long?,
    val canDelete: Boolean,
)
