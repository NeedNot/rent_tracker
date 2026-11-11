package net.neednot.renttracker.core.ui

data class UiMessage(
    val text: UiText,
    val variant: UiMessageVariant = UiMessageVariant.Default,
)
