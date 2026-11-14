package net.neednot.renttracker.feature.tenants.domain

data class OtherAllocation(
    val target: AllocationTarget,
    val amountCents: Long,
)
