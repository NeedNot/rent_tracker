package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.data.DuplicateMatchKind
import net.neednot.renttracker.core.designsystem.format.creditText
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.PendingBalanceChange
import net.neednot.renttracker.feature.tenants.domain.PendingDuplicateMatch
import net.neednot.renttracker.feature.tenants.domain.PendingNewEntry
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChangeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingMatchUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_payment_title
import net.neednot.renttracker.resources.pending_part_late_fee
import net.neednot.renttracker.resources.pending_part_you

internal fun PendingDuplicateMatch.toMatchUiState(): PendingMatchUiState =
    PendingMatchUiState(parts = parts(), amount = formatMoney(amountCents))

private fun PendingDuplicateMatch.parts(): List<UiText> {
    val day = UiText.Plain(formatMonthDay(date))
    val name = UiText.Plain(byName)
    return when (kind) {
        DuplicateMatchKind.LateFee -> listOf(UiText.Resource(Res.string.pending_part_late_fee), day, name)
        DuplicateMatchKind.Payment -> listOfNotNull(day, method?.let { UiText.Resource(it.label()) }, name)
    }
}

internal fun PendingBalanceChange.toChangeUiState(): PendingChangeUiState.Balance =
    PendingChangeUiState.Balance(was = balanceWords(wasCents), now = balanceWords(nowCents))

private fun balanceWords(signedCents: Long): UiText =
    when {
        signedCents > 0 -> owesText(signedCents)
        signedCents < 0 -> creditText(-signedCents)
        else -> UiText.Plain(formatMoney(0))
    }

internal fun PendingNewEntry.toChangeUiState(): PendingChangeUiState.NewEntry = PendingChangeUiState.NewEntry(parts())

private fun PendingNewEntry.parts(): List<UiText> =
    listOf(title(), UiText.Plain(formatMonthDay(date)), by.partText(), UiText.Plain(formatMoney(amountCents)))

private fun PendingNewEntry.title(): UiText =
    when (this) {
        is PendingNewEntry.Payment -> {
            UiText.Resource(Res.string.ledger_payment_title)
        }

        is PendingNewEntry.Charge -> {
            when (kind) {
                is ChargeKind.LateFee -> UiText.Resource(Res.string.pending_part_late_fee)
                is ChargeKind.Custom -> UiText.Plain(kind.note)
            }
        }
    }

private fun Person.partText(): UiText =
    when (this) {
        Person.You -> UiText.Resource(Res.string.pending_part_you)
        is Person.Named -> UiText.Plain(name)
    }
