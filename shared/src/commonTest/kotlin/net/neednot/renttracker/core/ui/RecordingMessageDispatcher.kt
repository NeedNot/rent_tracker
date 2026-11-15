package net.neednot.renttracker.core.ui

import kotlinx.coroutines.flow.emptyFlow

class RecordingMessageDispatcher : MessageDispatcher {
    val shown = mutableListOf<UiMessage>()

    override val messages = emptyFlow<UiMessage>()

    override fun show(message: UiMessage) {
        shown += message
    }
}
