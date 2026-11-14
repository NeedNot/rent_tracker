package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class GroupCardUiState(
    val groupId: String,
    val name: String,
    val balance: UiText,
    val isPaidUp: Boolean,
    val tenantCount: UiText,
    val lateCount: UiText?,
    val archivedOwed: UiText?,
)
