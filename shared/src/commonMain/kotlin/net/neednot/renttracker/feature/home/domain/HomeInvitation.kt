package net.neednot.renttracker.feature.home.domain

import net.neednot.renttracker.core.access.MemberPermissionLabel

data class HomeInvitation(
    val inviteId: String,
    val groupName: String,
    val invitedByName: String,
    val access: MemberPermissionLabel,
)
