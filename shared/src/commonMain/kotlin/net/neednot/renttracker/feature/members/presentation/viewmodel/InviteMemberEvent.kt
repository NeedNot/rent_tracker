package net.neednot.renttracker.feature.members.presentation.viewmodel

sealed interface InviteMemberEvent {
    data object InviteSent : InviteMemberEvent
}
