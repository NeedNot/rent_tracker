package net.neednot.renttracker.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DismissedLateFeeSuggestionDaoTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val dao = database.dismissedLateFeeSuggestionDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aDismissedMonthIsObservedForItsTenantOnly() =
        runTest {
            dao.insert(DismissedLateFeeSuggestionEntity("t1", "2026-03"))
            dao.insert(DismissedLateFeeSuggestionEntity("t2", "2026-04"))

            assertEquals(listOf("2026-03"), dao.observeDismissedMonths("t1").first())
        }

    @Test
    fun dismissingTheSameMonthTwiceKeepsOneRow() =
        runTest {
            dao.insert(DismissedLateFeeSuggestionEntity("t1", "2026-03"))
            dao.insert(DismissedLateFeeSuggestionEntity("t1", "2026-03"))

            assertEquals(listOf("2026-03"), dao.observeDismissedMonths("t1").first())
        }
}
