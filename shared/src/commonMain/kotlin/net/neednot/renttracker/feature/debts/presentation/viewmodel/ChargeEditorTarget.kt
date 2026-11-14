package net.neednot.renttracker.feature.debts.presentation.viewmodel

data class ChargeEditorTarget(
    val tenantId: String,
    val debtId: String? = null,
    val suggestion: ChargeSuggestion? = null,
)
