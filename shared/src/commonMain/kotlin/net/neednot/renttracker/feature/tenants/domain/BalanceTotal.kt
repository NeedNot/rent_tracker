package net.neednot.renttracker.feature.tenants.domain

data class BalanceTotal(
    val owedCents: Long,
    val creditCents: Long,
)
