package net.neednot.renttracker.feature.payments.presentation.viewmodel

import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState

internal fun PaymentEditorUiState.afterSaveFailure(error: PaymentError): PaymentEditorUiState =
    when (error) {
        is PaymentError.PossibleDuplicate -> {
            copy(duplicate = error.matches.toDialog(header?.tenantName.orEmpty(), offeredLateFee?.month))
        }

        is PaymentError.EditConflict -> {
            copy(conflictBanner = conflictBanner(error.editedByName))
        }

        else -> {
            val field = (error as? PaymentError.ValidationFailed)?.let { paymentEditorFieldOf(it.field) }
            if (field != null) copy(fieldErrors = fieldErrors + (field to error.text())) else copy(error = error.text())
        }
    }
