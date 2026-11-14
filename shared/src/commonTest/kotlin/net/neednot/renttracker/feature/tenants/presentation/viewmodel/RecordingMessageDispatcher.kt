package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.coroutines.flow.emptyFlow
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage

class RecordingMessageDispatcher : MessageDispatcher {
    val shown = mutableListOf<UiMessage>()

    override val messages = emptyFlow<UiMessage>()

    override fun show(message: UiMessage) {
        shown += message
    }
}
