package net.neednot.renttracker.feature.home.data

import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.data.HomeInvitationEntity
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.feature.home.domain.HomeInvitation

internal fun HomeInvitationEntity.toHomeInvitation(): HomeInvitation =
    HomeInvitation(
        inviteId = inviteId,
        groupName = groupName,
        invitedByName = invitedByName,
        access = MemberPermissionLabel.of(MembershipRole.Member, permissions),
    )
