package net.neednot.renttracker.feature.auth.presentation.viewmodel

import net.neednot.renttracker.feature.auth.domain.SignedInAccount

sealed interface SignedInDestination {
    data object Home : SignedInDestination

    data class DisplayName(
        val defaultDisplayName: String,
    ) : SignedInDestination
}

internal fun SignedInAccount.destination(): SignedInDestination =
    if (isNew) SignedInDestination.DisplayName(displayName) else SignedInDestination.Home
