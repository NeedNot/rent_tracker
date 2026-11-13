package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class ArchivedTenantRowUiState(
    val tenantId: String,
    val name: String,
    val contactName: String?,
    val balance: UiText,
)
