package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.PendingPayment
import net.neednot.renttracker.feature.tenants.domain.PendingState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingBannerUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_amount_paid
import net.neednot.renttracker.resources.ledger_payment_title
import net.neednot.renttracker.resources.pending_banner_attention
import net.neednot.renttracker.resources.pending_banner_waiting
import net.neednot.renttracker.resources.pending_banner_waiting_subtitle
import net.neednot.renttracker.resources.pending_row_not_saved
import net.neednot.renttracker.resources.pending_row_possible_duplicate
import net.neednot.renttracker.resources.pending_row_saved_on_phone
import net.neednot.renttracker.resources.pending_row_tenant_changed
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

internal fun PendingPayment.toRowUiState(): PendingRowUiState =
    PendingRowUiState(
        clientId = clientId,
        date = formatMonthDay(dateReceived),
        title = titleText(),
        chip = state.chip(),
        detail = UiText.Resource(state.rowDetail()),
        amount = UiText.Resource(Res.string.ledger_amount_paid, listOf(formatMoney(amountCents))),
    )

internal fun PendingPayment.titleText(): UiText =
    UiText.Resource(method?.paymentTitle() ?: Res.string.ledger_payment_title)

internal fun PendingState.chip(): PendingChip =
    when (this) {
        PendingState.Waiting -> PendingChip.Pending
        PendingState.PossibleDuplicate, PendingState.TenantChanged -> PendingChip.NeedsConfirmation
        is PendingState.Rejected -> PendingChip.Rejected
    }

private fun PendingState.rowDetail(): StringResource =
    when (this) {
        PendingState.Waiting -> Res.string.pending_row_saved_on_phone
        PendingState.PossibleDuplicate -> Res.string.pending_row_possible_duplicate
        PendingState.TenantChanged -> Res.string.pending_row_tenant_changed
        is PendingState.Rejected -> Res.string.pending_row_not_saved
    }

internal fun List<PendingPayment>.toBannerUiState(): PendingBannerUiState? {
    val needingAttention = filter { it.state != PendingState.Waiting }
    val firstToReview = needingAttention.firstOrNull()
    return when {
        firstToReview != null -> {
            PendingBannerUiState.Attention(
                title = countText(Res.plurals.pending_banner_attention, needingAttention.size),
                reviewClientId = firstToReview.clientId,
            )
        }

        isNotEmpty() -> {
            PendingBannerUiState.Waiting(
                title = countText(Res.plurals.pending_banner_waiting, size),
                subtitle = UiText.Resource(Res.string.pending_banner_waiting_subtitle),
            )
        }

        else -> {
            null
        }
    }
}

private fun countText(
    resource: PluralStringResource,
    count: Int,
): UiText = UiText.Plural(resource, count, listOf(count))
