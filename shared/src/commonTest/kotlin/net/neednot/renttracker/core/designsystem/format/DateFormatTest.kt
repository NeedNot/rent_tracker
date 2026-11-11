package net.neednot.renttracker.core.designsystem.format

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateFormatTest {
    @Test
    fun formatsUsDates() {
        val cases =
            listOf(
                "2026-03-02" to "Mar 2, 2026",
                "2026-01-31" to "Jan 31, 2026",
                "2028-02-29" to "Feb 29, 2028",
                "2026-12-10" to "Dec 10, 2026",
            )
        cases.forEach { (isoDate, expected) ->
            assertEquals(expected, formatDate(isoDate), "isoDate = $isoDate")
            assertEquals(expected, formatDate(LocalDate.parse(isoDate)), "LocalDate $isoDate")
        }
    }

    @Test
    fun rejectsDatesThatDoNotExist() {
        assertFailsWith<IllegalArgumentException> { formatDate("2026-02-30") }
    }
}
