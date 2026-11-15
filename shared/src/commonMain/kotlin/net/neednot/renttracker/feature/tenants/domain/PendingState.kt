package net.neednot.renttracker.feature.tenants.domain

sealed interface PendingState {
    data object Waiting : PendingState

    data object PossibleDuplicate : PendingState

    data object TenantChanged : PendingState

    data class Rejected(
        val reason: PendingRejection,
    ) : PendingState
}
