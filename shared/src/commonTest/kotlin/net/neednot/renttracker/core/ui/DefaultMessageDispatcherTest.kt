package net.neednot.renttracker.core.ui

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultMessageDispatcherTest {
    @Test
    fun deliversMessagesInOrder() =
        runTest {
            val dispatcher = DefaultMessageDispatcher()
            val first = UiMessage(UiText.Plain("Saved"))
            val second = UiMessage(UiText.Plain("Could not save"), UiMessageVariant.Error)

            dispatcher.messages.test {
                dispatcher.show(first)
                dispatcher.show(second)

                assertEquals(first, awaitItem())
                assertEquals(second, awaitItem())
            }
        }
}
