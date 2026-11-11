package net.neednot.renttracker.core.designsystem.format

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_credit
import net.neednot.renttracker.resources.balance_owes

private const val CENTS_PER_DOLLAR = 100
private const val DIGITS_PER_GROUP = 3

fun formatMoney(cents: Long): String {
    require(cents >= 0) { "Money amounts are never negative: $cents" }
    val dollars = (cents / CENTS_PER_DOLLAR).toString()
    val remainder = (cents % CENTS_PER_DOLLAR).toString().padStart(2, '0')
    return "$${groupThousands(dollars)}.$remainder"
}

fun owesText(cents: Long): UiText = UiText.Resource(Res.string.balance_owes, listOf(formatMoney(cents)))

fun creditText(cents: Long): UiText = UiText.Resource(Res.string.balance_credit, listOf(formatMoney(cents)))

private fun groupThousands(digits: String): String =
    digits
        .reversed()
        .chunked(DIGITS_PER_GROUP)
        .joinToString(",")
        .reversed()
