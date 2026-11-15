package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class MoveTargetUiState(
    val groupId: String,
    val name: String,
    val tenantCount: UiText,
    val isSelected: Boolean,
)
