package net.neednot.renttracker.feature.home.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.RejectedPayment
import net.neednot.renttracker.feature.home.domain.RejectedPaymentReason
import net.neednot.renttracker.feature.home.presentation.uistate.RejectedPaymentUiState
import net.neednot.renttracker.feature.home.presentation.uistate.RejectedPaymentsUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_rejected_payment_title
import net.neednot.renttracker.resources.home_rejected_payments_summary
import net.neednot.renttracker.resources.pending_rejected_archived
import net.neednot.renttracker.resources.pending_rejected_moved
import net.neednot.renttracker.resources.pending_rejected_no_permission

internal fun List<RejectedPayment>.toRejectedPaymentsUiState(isSheetOpen: Boolean): RejectedPaymentsUiState? {
    if (isEmpty()) return null
    return RejectedPaymentsUiState(
        summary = UiText.Plural(Res.plurals.home_rejected_payments_summary, size, listOf(size)),
        payments = map { it.toUiState() },
        isSheetOpen = isSheetOpen,
    )
}

private fun RejectedPayment.toUiState(): RejectedPaymentUiState =
    RejectedPaymentUiState(
        clientId = clientId,
        title = UiText.Resource(Res.string.home_rejected_payment_title, listOf(tenantName)),
        amount = formatMoney(amountCents),
        dateReceived = formatDate(dateReceived),
        reason = reasonText(),
    )

private fun RejectedPayment.reasonText(): UiText =
    when (reason) {
        RejectedPaymentReason.NoPermission -> UiText.Resource(Res.string.pending_rejected_no_permission)
        RejectedPaymentReason.TenantArchived -> UiText.Resource(Res.string.pending_rejected_archived)
        RejectedPaymentReason.TenantMoved -> UiText.Resource(Res.string.pending_rejected_moved)
        RejectedPaymentReason.Other -> UiText.Plain(message.orEmpty())
    }
