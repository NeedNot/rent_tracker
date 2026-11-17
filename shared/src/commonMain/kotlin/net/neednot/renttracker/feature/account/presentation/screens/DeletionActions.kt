package net.neednot.renttracker.feature.account.presentation.screens

internal data class DeletionActions(
    val onClicked: () -> Unit = {},
    val onConfirmed: () -> Unit = {},
    val onDismissed: () -> Unit = {},
)
