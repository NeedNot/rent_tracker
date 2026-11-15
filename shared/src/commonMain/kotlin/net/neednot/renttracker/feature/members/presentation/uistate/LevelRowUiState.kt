package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.data.AccessLevel

data class LevelRowUiState(
    val area: PermissionArea,
    val levels: List<AccessLevel>,
    val selected: AccessLevel,
)
