package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.ChargeDetails
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.EntryEdit
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.PaymentDetails
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryEditTarget
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerFieldUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_date_by_named
import net.neednot.renttracker.resources.ledger_date_by_you
import net.neednot.renttracker.resources.ledger_edited_named
import net.neednot.renttracker.resources.ledger_edited_you
import net.neednot.renttracker.resources.ledger_field_added
import net.neednot.renttracker.resources.ledger_field_cancelled
import net.neednot.renttracker.resources.ledger_field_date
import net.neednot.renttracker.resources.ledger_field_date_received
import net.neednot.renttracker.resources.ledger_field_edited
import net.neednot.renttracker.resources.ledger_field_for_month
import net.neednot.renttracker.resources.ledger_field_logged
import net.neednot.renttracker.resources.ledger_field_marked_bounced
import net.neednot.renttracker.resources.ledger_field_method
import net.neednot.renttracker.resources.ledger_field_note
import net.neednot.renttracker.resources.ledger_field_reason
import net.neednot.renttracker.resources.ledger_field_status
import net.neednot.renttracker.resources.ledger_sheet_late_fee
import net.neednot.renttracker.resources.ledger_sheet_not_counted
import net.neednot.renttracker.resources.ledger_sheet_payment
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Instant

internal fun PaymentDetails.fields(timeZone: TimeZone): List<LedgerFieldUiState> =
    listOfNotNull(
        notCountedField(status),
        field(Res.string.ledger_field_date_received, UiText.Plain(formatDate(dateReceived))),
        method?.let { field(Res.string.ledger_field_method, UiText.Resource(it.label())) },
        noteField(note),
        field(Res.string.ledger_field_logged, dateBy(loggedBy, loggedAt, timeZone)),
    ) + statusFields(status, timeZone) + listOfNotNull(editedField(edit, timeZone))

internal fun ChargeDetails.fields(timeZone: TimeZone): List<LedgerFieldUiState> =
    listOfNotNull(
        notCountedField(status),
        (kind as? ChargeKind.LateFee)?.let {
            field(Res.string.ledger_field_for_month, UiText.Plain(fullMonthAndYear(it.forMonth)))
        },
        field(Res.string.ledger_field_date, UiText.Plain(formatDate(date))),
        noteField(note.takeIf { kind is ChargeKind.LateFee }),
        field(Res.string.ledger_field_added, dateBy(addedBy, addedAt, timeZone)),
    ) + statusFields(status, timeZone) + listOfNotNull(editedField(edit, timeZone))

private fun notCountedField(status: EntryStatus): LedgerFieldUiState? =
    if (status == EntryStatus.Active) {
        null
    } else {
        field(Res.string.ledger_field_status, UiText.Resource(Res.string.ledger_sheet_not_counted))
    }

private fun noteField(note: String?): LedgerFieldUiState? =
    note?.takeIf { it.isNotBlank() }?.let { field(Res.string.ledger_field_note, UiText.Plain(it)) }

private fun statusFields(
    status: EntryStatus,
    timeZone: TimeZone,
): List<LedgerFieldUiState> =
    when (status) {
        EntryStatus.Active -> {
            emptyList()
        }

        is EntryStatus.Bounced -> {
            listOf(field(Res.string.ledger_field_marked_bounced, dateBy(status.by, status.at, timeZone)))
        }

        is EntryStatus.Cancelled -> {
            listOfNotNull(
                field(Res.string.ledger_field_cancelled, dateBy(status.by, status.at, timeZone)),
                status.reason.takeIf { it.isNotBlank() }?.let {
                    field(
                        Res.string.ledger_field_reason,
                        UiText.Plain(it),
                    )
                },
            )
        }
    }

private fun editedField(
    edit: EntryEdit?,
    timeZone: TimeZone,
): LedgerFieldUiState? =
    edit?.let {
        val day = formatMonthDay(it.at.localDate(timeZone))
        field(
            Res.string.ledger_field_edited,
            it.by.text(Res.string.ledger_edited_you, Res.string.ledger_edited_named, day),
        )
    }

private fun dateBy(
    person: Person,
    at: Instant,
    timeZone: TimeZone,
): UiText =
    person.text(Res.string.ledger_date_by_you, Res.string.ledger_date_by_named, formatDate(at.localDate(timeZone)))

private fun field(
    label: StringResource,
    value: UiText,
): LedgerFieldUiState = LedgerFieldUiState(label = UiText.Resource(label), value = value)
