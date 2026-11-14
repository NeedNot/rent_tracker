package net.neednot.renttracker.feature.payments.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.format.formatDollarsInput
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.domain.PaymentEntryOverview
import net.neednot.renttracker.feature.payments.presentation.uistate.LateFeeOfferUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentHeaderUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up

internal fun PaymentEditorUiState.showing(overview: PaymentEntryOverview): PaymentEditorUiState {
    val saved = overview.payment?.details
    return copy(
        isLoading = false,
        header = overview.toHeader(),
        today = overview.today,
        amount = saved?.let { formatDollarsInput(it.amountCents) }.orEmpty(),
        date = saved?.dateReceived ?: overview.today,
        method = if (saved == null) overview.lastPaymentMethod else saved.method,
        lastUsedMethod = if (saved == null) overview.lastPaymentMethod else null,
        note = saved?.note.orEmpty(),
        lateFeeOffer = if (saved == null) overview.lateFeeOffer() else null,
        fieldErrors = emptyMap(),
        duplicate = null,
    )
}

private fun PaymentEntryOverview.toHeader(): PaymentHeaderUiState =
    PaymentHeaderUiState(
        tenantName = tenantName,
        groupName = groupName,
        balance = balance?.inWords() ?: UiText.Resource(Res.string.balance_paid_up),
    )

private fun PaymentEntryOverview.lateFeeOffer(): LateFeeOfferUiState? =
    lateFeeSuggestion?.takeIf { canAddDebts }?.let {
        LateFeeOfferUiState(month = it.forMonth, isChecked = false, amount = formatDollarsInput(it.amountCents))
    }
