package net.neednot.renttracker.core.sync

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncedFlowTest {
    private val cache = MutableStateFlow(listOf("cached"))
    private val serverResults = MutableSharedFlow<Result<List<String>, String>>()
    private val synced = syncedFlow(cached = cache, serverResults = serverResults, store = { cache.value = it })

    @Test
    fun showsTheCacheWhileWaitingForTheServer() =
        runTest {
            synced.test {
                assertEquals(Synced(listOf("cached"), SyncStatus.Syncing), awaitItem())
            }
        }

    @Test
    fun aServerResultIsStoredBeforeItIsReportedAsSynced() =
        runTest {
            synced.test {
                skipItems(1)

                serverResults.emit(Ok(listOf("fresh")))

                assertEquals(Synced(listOf("fresh"), SyncStatus.Synced), expectMostRecentItem())
            }
        }

    @Test
    fun aServerErrorKeepsTheCacheAndReportsTheFailure() =
        runTest {
            synced.test {
                skipItems(1)

                serverResults.emit(Err("offline"))

                assertEquals(Synced(listOf("cached"), SyncStatus.Failed("offline")), awaitItem())
            }
        }

    @Test
    fun laterCacheChangesKeepFlowingWithTheLastStatus() =
        runTest {
            synced.test {
                skipItems(1)
                serverResults.emit(Ok(listOf("fresh")))
                skipItems(1)

                cache.value = listOf("edited")

                assertEquals(Synced(listOf("edited"), SyncStatus.Synced), expectMostRecentItem())
            }
        }

    @Test
    fun theServerSubscriptionClosesWhenNobodyCollects() =
        runTest {
            synced.test {
                skipItems(1)
                assertEquals(1, serverResults.subscriptionCount.value)
            }

            assertEquals(0, serverResults.subscriptionCount.value)
        }
}
