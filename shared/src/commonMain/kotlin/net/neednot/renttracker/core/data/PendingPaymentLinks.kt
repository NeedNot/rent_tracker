package net.neednot.renttracker.core.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class PendingPaymentLinks {
    private val links = Channel<PendingPaymentLink>(Channel.CONFLATED)

    val opened: Flow<PendingPaymentLink> = links.receiveAsFlow()

    fun open(link: PendingPaymentLink) {
        links.trySend(link)
    }
}
