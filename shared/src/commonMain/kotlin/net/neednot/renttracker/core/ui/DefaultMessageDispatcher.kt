package net.neednot.renttracker.core.ui

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class DefaultMessageDispatcher : MessageDispatcher {
    private val pendingMessages =
        Channel<UiMessage>(capacity = BUFFER_CAPACITY, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val messages: Flow<UiMessage> = pendingMessages.receiveAsFlow()

    override fun show(message: UiMessage) {
        pendingMessages.trySend(message)
    }

    private companion object {
        const val BUFFER_CAPACITY = 8
    }
}
