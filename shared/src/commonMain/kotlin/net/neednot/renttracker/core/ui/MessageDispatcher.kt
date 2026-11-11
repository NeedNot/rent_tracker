package net.neednot.renttracker.core.ui

import kotlinx.coroutines.flow.Flow

interface MessageDispatcher {
    val messages: Flow<UiMessage>

    fun show(message: UiMessage)
}
