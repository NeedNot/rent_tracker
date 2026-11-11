package net.neednot.renttracker.core.designsystem.format

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_credit
import net.neednot.renttracker.resources.balance_owes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyFormatTest {
    @Test
    fun formatsCentsAsDollars() {
        val cases =
            listOf(
                0L to "$0.00",
                5L to "$0.05",
                99L to "$0.99",
                100L to "$1.00",
                123_450L to "$1,234.50",
                150_000L to "$1,500.00",
                99_999_999L to "$999,999.99",
                100_000_000L to "$1,000,000.00",
            )
        cases.forEach { (cents, expected) -> assertEquals(expected, formatMoney(cents), "cents = $cents") }
    }

    @Test
    fun rejectsNegativeAmounts() {
        assertFailsWith<IllegalArgumentException> { formatMoney(-1) }
    }

    @Test
    fun balanceWordsCarryTheFormattedAmount() {
        assertEquals(UiText.Resource(Res.string.balance_owes, listOf("$300.00")), owesText(30_000))
        assertEquals(UiText.Resource(Res.string.balance_credit, listOf("$300.00")), creditText(30_000))
    }
}
