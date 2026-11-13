package net.neednot.renttracker.feature.tenants.presentation.uistate

import kotlinx.datetime.LocalDate

data class TenantEditorForm(
    val name: String = "",
    val contactName: String = "",
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    val securityDeposit: String = "",
    val moveIn: LocalDate? = null,
    val lastDayInUnit: LocalDate? = null,
    val graceDays: String = "",
    val lateFee: String = "",
    val monthlyRent: String = "",
    val dueDay: String = "",
)
