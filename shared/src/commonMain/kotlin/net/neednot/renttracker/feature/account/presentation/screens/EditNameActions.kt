package net.neednot.renttracker.feature.account.presentation.screens

internal data class EditNameActions(
    val onClicked: () -> Unit = {},
    val onChanged: (String) -> Unit = {},
    val onSaved: () -> Unit = {},
    val onDismissed: () -> Unit = {},
)
