package net.neednot.renttracker.core.designsystem.format

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_error_amount_format
import net.neednot.renttracker.resources.common_error_amount_positive

internal fun amountFormatError(text: String): UiText? =
    if (parseDollarsToCents(text) == null) UiText.Resource(Res.string.common_error_amount_format) else null

internal fun positiveAmountFormatError(text: String): UiText? =
    if (parseDollarsToCents(text) == 0L) {
        UiText.Resource(Res.string.common_error_amount_positive)
    } else {
        amountFormatError(text)
    }
