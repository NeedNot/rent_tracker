package net.neednot.renttracker.core.validation

// Mirrors convex/inputValidation.ts so the app can flag these before the server does.
object InputLimits {
    const val MAX_AMOUNT_CENTS = 100_000_000L
    const val MAX_NAME_LENGTH = 100
    const val MAX_NOTES_LENGTH = 2000
    val DUE_DAYS = 1..31
    val GRACE_DAYS = 0..15
}
