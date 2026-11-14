package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.designsystem.components.TenantChipState
import net.neednot.renttracker.core.ui.UiText

data class TenantRowUiState(
    val tenantId: String,
    val name: String,
    val detail: UiText?,
    val balance: UiText?,
    val chipState: TenantChipState,
)
