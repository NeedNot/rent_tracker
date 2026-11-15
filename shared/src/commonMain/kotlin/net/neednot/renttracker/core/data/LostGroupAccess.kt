package net.neednot.renttracker.core.data

data class LostGroupAccess(
    val groupId: String,
    val groupName: String,
    val reason: AccessLossReason,
)
