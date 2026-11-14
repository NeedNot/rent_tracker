package net.neednot.renttracker.feature.debts.domain

data class SavedCharge(
    val debtId: String,
    val version: Long,
    val draft: ChargeDraft,
)
