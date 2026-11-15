package net.neednot.renttracker.feature.members.presentation.viewmodel

sealed interface TransferOwnershipEvent {
    data object OwnershipTransferred : TransferOwnershipEvent
}
