package net.neednot.renttracker.feature.debts.presentation.viewmodel

internal data class ChargeSaveRequest(
    val tenantId: String,
    val debtId: String?,
    val clientId: String,
    val expectedVersion: Long?,
)
