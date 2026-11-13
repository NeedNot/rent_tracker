package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class TenantDetailsUiState(
    val contactName: String?,
    val phone: String?,
    val email: String?,
    val movedIn: String,
    val lastDayInUnit: String?,
    val securityDeposit: String?,
    val gracePeriod: UiText,
    val lateFee: UiText,
    val notes: String?,
)
