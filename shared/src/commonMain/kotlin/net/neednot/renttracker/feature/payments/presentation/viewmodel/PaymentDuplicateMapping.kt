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
import net.neednot.renttracker.resources.payment_duplicate_intro_both
import net.neednot.renttracker.resources.payment_duplicate_intro_fee
import net.neednot.renttracker.resources.payment_duplicate_intro_payment
import net.neednot.renttracker.resources.payment_duplicate_intro_payments
import net.neednot.renttracker.resources.payment_duplicate_kind_late_fee
import net.neednot.renttracker.resources.payment_duplicate_kind_payment
import net.neednot.renttracker.resources.payment_duplicate_question_both
import net.neednot.renttracker.resources.payment_duplicate_question_one
import net.neednot.renttracker.resources.payment_duplicate_title_many
import net.neednot.renttracker.resources.payment_duplicate_title_one
import net.neednot.renttracker.resources.payment_duplicate_you

internal fun List<DuplicateMatch>.toDialog(
    callerAccountId: String?,
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
        intro = introFor(hasPayment, hasLateFee, lateFeeMonth),
        matches = map { it.toUiState(callerAccountId, lateFeeMonth, labelsKinds) },
        question =
            UiText.Resource(
                if (labelsKinds) {
                    Res.string.payment_duplicate_question_both
                } else {
                    Res.string.payment_duplicate_question_one
                },
            ),
    )
}

private fun List<DuplicateMatch>.introFor(
    hasPayment: Boolean,
    hasLateFee: Boolean,
    lateFeeMonth: YearMonth?,
): UiText =
    when {
        hasPayment && hasLateFee -> {
            UiText.Resource(Res.string.payment_duplicate_intro_both)
        }

        hasLateFee -> {
            UiText.Resource(
                Res.string.payment_duplicate_intro_fee,
                listOf(lateFeeMonth?.monthName().orEmpty()),
            )
        }

        size > 1 -> {
            UiText.Resource(Res.string.payment_duplicate_intro_payments)
        }

        else -> {
            UiText.Resource(Res.string.payment_duplicate_intro_payment)
        }
    }

private fun DuplicateMatch.toUiState(
    callerAccountId: String?,
    lateFeeMonth: YearMonth?,
    labelsKind: Boolean,
): DuplicateMatchUiState =
    DuplicateMatchUiState(
        kind = if (labelsKind) kindLabel(lateFeeMonth) else null,
        date = formatMonthDay(date),
        method = method,
        by =
            if (callerAccountId != null &&
                byAccountId == callerAccountId
            ) {
                UiText.Resource(Res.string.payment_duplicate_you)
            } else {
                UiText.Plain(byName)
            },
        amount = formatMoney(amountCents),
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
