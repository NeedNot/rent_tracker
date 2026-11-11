package net.neednot.renttracker.core.ui

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DefaultMessageDispatcher : MessageDispatcher {
    // Buffered so show() never suspends and a message sent while the host is briefly detached is not lost.
    private val pendingMessages =
        MutableSharedFlow<UiMessage>(
            extraBufferCapacity = BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    override val messages: SharedFlow<UiMessage> = pendingMessages.asSharedFlow()

    override fun show(message: UiMessage) {
        pendingMessages.tryEmit(message)
    }

    private companion object {
        const val BUFFER_CAPACITY = 8
    }
}
