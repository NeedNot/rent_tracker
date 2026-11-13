package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_editor_error_amount_format
import net.neednot.renttracker.resources.tenant_editor_error_amount_positive

internal fun amountFormatError(text: String): UiText? =
    if (parseDollarsToCents(text) == null) UiText.Resource(Res.string.tenant_editor_error_amount_format) else null

internal fun positiveAmountFormatError(text: String): UiText? =
    if (parseDollarsToCents(text) == 0L) {
        UiText.Resource(Res.string.tenant_editor_error_amount_positive)
    } else {
        amountFormatError(text)
    }
