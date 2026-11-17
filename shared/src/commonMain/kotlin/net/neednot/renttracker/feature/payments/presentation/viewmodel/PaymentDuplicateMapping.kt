package net.neednot.renttracker.feature.payments.presentation.viewmodel

import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.domain.DuplicateKind
import net.neednot.renttracker.feature.payments.domain.DuplicateMatch
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateDialogUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateMatchUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_duplicate_fee
import net.neednot.renttracker.resources.payment_duplicate_intro_both
import net.neednot.renttracker.resources.payment_duplicate_intro_fee
import net.neednot.renttracker.resources.payment_duplicate_intro_payment
import net.neednot.renttracker.resources.payment_duplicate_intro_payments
import net.neednot.renttracker.resources.payment_duplicate_kind_late_fee
import net.neednot.renttracker.resources.payment_duplicate_kind_payment
import net.neednot.renttracker.resources.payment_duplicate_paid
import net.neednot.renttracker.resources.payment_duplicate_title_many
import net.neednot.renttracker.resources.payment_duplicate_title_one

internal fun List<DuplicateMatch>.toDialog(
    tenantName: String,
    lateFeeMonth: YearMonth?,
): DuplicateDialogUiState {
    val hasPayment = any { it.kind == DuplicateKind.Payment }
    val hasLateFee = any { it.kind == DuplicateKind.LateFee }
    val labelsKinds = hasPayment && hasLateFee
    return DuplicateDialogUiState(
        title =
            UiText.Resource(
                if (size > 1) Res.string.payment_duplicate_title_many else Res.string.payment_duplicate_title_one,
            ),
        intro = introFor(hasPayment, hasLateFee, tenantName, lateFeeMonth),
        matches = map { it.toUiState(lateFeeMonth, labelsKinds) },
    )
}

private fun List<DuplicateMatch>.introFor(
    hasPayment: Boolean,
    hasLateFee: Boolean,
    tenantName: String,
    lateFeeMonth: YearMonth?,
): UiText =
    when {
        hasPayment && hasLateFee -> {
            UiText.Resource(Res.string.payment_duplicate_intro_both, listOf(tenantName))
        }

        hasLateFee -> {
            UiText.Resource(
                Res.string.payment_duplicate_intro_fee,
                listOf(lateFeeMonth?.monthName().orEmpty(), tenantName),
            )
        }

        size > 1 -> {
            UiText.Resource(Res.string.payment_duplicate_intro_payments, listOf(tenantName))
        }

        else -> {
            UiText.Resource(Res.string.payment_duplicate_intro_payment, listOf(tenantName))
        }
    }

private fun DuplicateMatch.toUiState(
    lateFeeMonth: YearMonth?,
    labelsKind: Boolean,
): DuplicateMatchUiState =
    DuplicateMatchUiState(
        kind = if (labelsKind) kindLabel(lateFeeMonth) else null,
        date = formatMonthDay(date),
        method = method,
        amount =
            UiText.Resource(
                when (kind) {
                    DuplicateKind.Payment -> Res.string.payment_duplicate_paid
                    DuplicateKind.LateFee -> Res.string.payment_duplicate_fee
                },
                listOf(formatMoney(amountCents)),
            ),
    )

private fun DuplicateMatch.kindLabel(lateFeeMonth: YearMonth?): UiText =
    when (kind) {
        DuplicateKind.Payment -> {
            UiText.Resource(Res.string.payment_duplicate_kind_payment)
        }

        DuplicateKind.LateFee -> {
            UiText.Resource(Res.string.payment_duplicate_kind_late_fee, listOf(lateFeeMonth?.monthName().orEmpty()))
        }
    }
