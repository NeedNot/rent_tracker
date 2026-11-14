package net.neednot.renttracker.core.designsystem.format

private const val CENTS_PER_DOLLAR = 100L
private const val CENT_DIGITS = 2

private val DollarsPattern = Regex("""(\d{0,13})(?:\.(\d{0,2}))?""")

fun parseDollarsToCents(text: String): Long? {
    val typed = text.trim().removePrefix("$").replace(",", "")
    val (dollars, cents) = DollarsPattern.matchEntire(typed)?.destructured ?: return null
    val hasDigits = dollars.isNotEmpty() || cents.isNotEmpty()
    return if (hasDigits) {
        dollars
            .ifEmpty {
                "0"
            }.toLong() * CENTS_PER_DOLLAR + cents.padEnd(CENT_DIGITS, '0').toLong()
    } else {
        null
    }
}

fun formatDollarsInput(cents: Long): String = formatMoney(cents).removePrefix("$")
