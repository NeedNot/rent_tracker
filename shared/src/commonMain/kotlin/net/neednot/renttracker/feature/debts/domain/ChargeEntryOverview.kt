package net.neednot.renttracker.feature.debts.domain

import kotlinx.datetime.LocalDate

data class ChargeEntryOverview(
    val tenantName: String,
    val groupName: String,
    val balance: DebtBalance?,
    val today: LocalDate,
    val callerAccountId: String?,
    val charge: SavedCharge?,
)
