package net.neednot.renttracker.feature.account.presentation.screens

internal data class AccountActions(
    val onBackClicked: () -> Unit = {},
    val onSignOutClicked: () -> Unit = {},
    val signOutWarning: SignOutWarningActions = SignOutWarningActions(),
    val editName: EditNameActions = EditNameActions(),
    val deletion: DeletionActions = DeletionActions(),
)
