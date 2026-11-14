package net.neednot.renttracker.core.validation

import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.input_error_amount_format
import net.neednot.renttracker.resources.input_error_amount_positive
import net.neednot.renttracker.resources.input_error_amount_too_large
import net.neednot.renttracker.resources.input_error_too_long
import org.jetbrains.compose.resources.StringResource

fun amountError(
    text: String,
    allowsZero: Boolean,
): UiText? {
    if (text.isBlank()) return null
    val cents = parseDollarsToCents(text)
    return when {
        cents == null -> UiText.Resource(Res.string.input_error_amount_format)
        cents == 0L && !allowsZero -> UiText.Resource(Res.string.input_error_amount_positive)
        cents > InputLimits.MAX_AMOUNT_CENTS -> UiText.Resource(Res.string.input_error_amount_too_large)
        else -> null
    }
}

fun wholeNumberError(
    text: String,
    range: IntRange,
    message: StringResource,
): UiText? = if (text.isBlank() || text.trim().toIntOrNull() in range) null else UiText.Resource(message)

fun lengthError(
    text: String,
    maxLength: Int,
): UiText? =
    if (text.trim().length > maxLength) UiText.Resource(Res.string.input_error_too_long, listOf(maxLength)) else null
