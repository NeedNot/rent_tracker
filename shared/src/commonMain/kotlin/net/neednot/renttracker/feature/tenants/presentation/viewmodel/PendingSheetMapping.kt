package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.PendingPayment
import net.neednot.renttracker.feature.tenants.domain.PendingPaymentReview
import net.neednot.renttracker.feature.tenants.domain.PendingRejection
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerFieldUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetBody
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_field_date_received
import net.neednot.renttracker.resources.ledger_field_method
import net.neednot.renttracker.resources.pending_changed_intro
import net.neednot.renttracker.resources.pending_changed_title
import net.neednot.renttracker.resources.pending_duplicate_intro
import net.neednot.renttracker.resources.pending_duplicate_title
import net.neednot.renttracker.resources.pending_rejected_archived
import net.neednot.renttracker.resources.pending_rejected_moved
import net.neednot.renttracker.resources.pending_rejected_no_permission
import net.neednot.renttracker.resources.pending_rejected_not_saved
import net.neednot.renttracker.resources.pending_sheet_not_sent
import net.neednot.renttracker.resources.pending_sheet_title

internal fun PendingPaymentReview.toSheetUiState(): PendingSheetUiState =
    PendingSheetUiState(
        clientId = payment.clientId,
        title = UiText.Resource(Res.string.pending_sheet_title, listOf(payment.tenantName)),
        chip = payment.state.chip(),
        amount = formatMoney(payment.amountCents),
        body = body(),
        actions = actions(),
    )

private fun PendingPaymentReview.body(): PendingSheetBody =
    when (this) {
        is PendingPaymentReview.Waiting -> {
            PendingSheetBody.Waiting(
                message = UiText.Resource(Res.string.pending_sheet_not_sent),
                fields = payment.fields(),
            )
        }

        is PendingPaymentReview.PossibleDuplicate -> {
            PendingSheetBody.PossibleDuplicate(
                title = UiText.Resource(Res.string.pending_duplicate_title),
                intro = UiText.Resource(Res.string.pending_duplicate_intro, listOf(payment.tenantName)),
                matches = matches.map { it.toMatchUiState() },
            )
        }

        is PendingPaymentReview.TenantChanged -> {
            PendingSheetBody.TenantChanged(
                title = UiText.Resource(Res.string.pending_changed_title),
                intro = UiText.Resource(Res.string.pending_changed_intro),
                changes =
                    listOfNotNull(balanceChange?.toChangeUiState()) +
                        newEntries.map { it.toChangeUiState() },
            )
        }

        is PendingPaymentReview.Rejected -> {
            PendingSheetBody.Rejected(
                reason = reason.text(),
                notSaved = UiText.Resource(Res.string.pending_rejected_not_saved),
            )
        }
    }

private fun PendingPaymentReview.actions(): List<PendingSheetAction> =
    when (this) {
        is PendingPaymentReview.Waiting -> {
            listOf(PendingSheetAction.Edit, PendingSheetAction.Cancel)
        }

        is PendingPaymentReview.PossibleDuplicate -> {
            listOf(PendingSheetAction.SaveAnyway, PendingSheetAction.CancelPayment)
        }

        is PendingPaymentReview.TenantChanged -> {
            listOf(PendingSheetAction.SendAnyway, PendingSheetAction.Edit, PendingSheetAction.CancelPayment)
        }

        is PendingPaymentReview.Rejected -> {
            listOf(PendingSheetAction.Dismiss)
        }
    }

private fun PendingPayment.fields(): List<LedgerFieldUiState> =
    listOfNotNull(
        LedgerFieldUiState(
            label = UiText.Resource(Res.string.ledger_field_date_received),
            value = UiText.Plain(formatDate(dateReceived)),
        ),
        method?.let { method ->
            LedgerFieldUiState(UiText.Resource(Res.string.ledger_field_method), UiText.Resource(method.label()))
        },
    )

private fun PendingRejection.text(): UiText =
    when (this) {
        PendingRejection.NoPermission -> UiText.Resource(Res.string.pending_rejected_no_permission)
        PendingRejection.TenantArchived -> UiText.Resource(Res.string.pending_rejected_archived)
        PendingRejection.TenantMoved -> UiText.Resource(Res.string.pending_rejected_moved)
        is PendingRejection.Other -> UiText.Plain(message)
    }
