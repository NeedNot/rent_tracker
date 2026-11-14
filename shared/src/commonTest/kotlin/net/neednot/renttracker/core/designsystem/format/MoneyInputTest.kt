package net.neednot.renttracker.core.designsystem.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyInputTest {
    @Test
    fun readsTypedDollarsAsWholeCents() {
        val cases =
            listOf(
                "0" to 0L,
                "5" to 500L,
                "5." to 500L,
                ".5" to 50L,
                "0.05" to 5L,
                "1500" to 150_000L,
                "1500.5" to 150_050L,
                "1,500.00" to 150_000L,
                " $1,234.56 " to 123_456L,
                "0.29" to 29L,
                "1000000.00" to 100_000_000L,
                "9999999999999.99" to 999_999_999_999_999L,
            )
        cases.forEach { (text, expected) -> assertEquals(expected, parseDollarsToCents(text), "text = $text") }
    }

    @Test
    fun rejectsTextThatIsNotADollarAmount() {
        val cases = listOf("", "  ", ".", "abc", "-5", "+5", "1.234", "1.2.3", "5 dollars", "1e3", "12345678901234")
        cases.forEach { text -> assertNull(parseDollarsToCents(text), "text = $text") }
    }

    @Test
    fun formatsCentsForAnAmountFieldWithoutTheDollarSign() {
        assertEquals("1,500.00", formatDollarsInput(150_000))
        assertEquals("0.05", formatDollarsInput(5))
    }

    @Test
    fun aFormattedAmountReadsBackAsTheSameCents() {
        listOf(0L, 5L, 29L, 150_000L, 100_000_000L).forEach { cents ->
            assertEquals(cents, parseDollarsToCents(formatDollarsInput(cents)))
        }
    }
}
