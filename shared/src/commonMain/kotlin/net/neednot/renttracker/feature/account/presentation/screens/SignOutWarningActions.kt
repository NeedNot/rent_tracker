package net.neednot.renttracker.feature.account.presentation.screens

internal data class SignOutWarningActions(
    val onConfirmed: () -> Unit = {},
    val onDismissed: () -> Unit = {},
)
