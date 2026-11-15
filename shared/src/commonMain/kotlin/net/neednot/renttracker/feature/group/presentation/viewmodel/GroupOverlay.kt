package net.neednot.renttracker.feature.group.presentation.viewmodel

internal enum class GroupDialog {
    Archive,
    Leave,
    LeaveOwner,
}

internal data class GroupOverlay(
    val dialog: GroupDialog? = null,
    val isWorking: Boolean = false,
)
