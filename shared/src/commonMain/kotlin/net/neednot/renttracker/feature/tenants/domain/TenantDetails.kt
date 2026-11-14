package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate

data class TenantDetails(
    val name: String,
    val contactName: String?,
    val contactPhone: String?,
    val contactEmail: String?,
    val notes: String?,
    val depositCents: Long?,
    val moveIn: LocalDate,
    val moveOut: LocalDate?,
    val graceDaysOverride: Int?,
    val lateFeeFlatOverrideCents: Long?,
)
