package net.neednot.renttracker.feature.debts.domain

data class DebtBalance(
    val owedCents: Long,
    val creditCents: Long,
)
