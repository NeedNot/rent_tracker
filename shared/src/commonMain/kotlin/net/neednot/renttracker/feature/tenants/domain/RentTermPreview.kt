package net.neednot.renttracker.feature.tenants.domain

data class RentTermPreview(
    val before: BalanceTotal,
    val after: BalanceTotal,
)
