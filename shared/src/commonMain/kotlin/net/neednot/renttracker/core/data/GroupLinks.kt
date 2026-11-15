package net.neednot.renttracker.core.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class GroupLinks {
    private val links = Channel<String>(Channel.CONFLATED)

    val opened: Flow<String> = links.receiveAsFlow()

    fun open(groupId: String) {
        links.trySend(groupId)
    }
}
