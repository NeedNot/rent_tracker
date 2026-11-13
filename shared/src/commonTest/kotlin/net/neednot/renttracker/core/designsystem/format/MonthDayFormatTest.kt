package net.neednot.renttracker.core.designsystem.format

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthDayFormatTest {
    @Test
    fun formatsTheMonthAndDayWithoutTheYear() {
        val cases =
            listOf(
                "2026-04-01" to "Apr 1",
                "2026-12-31" to "Dec 31",
                "2028-02-29" to "Feb 29",
            )
        cases.forEach { (isoDate, expected) ->
            assertEquals(expected, formatMonthDay(LocalDate.parse(isoDate)), "isoDate = $isoDate")
        }
    }
}
