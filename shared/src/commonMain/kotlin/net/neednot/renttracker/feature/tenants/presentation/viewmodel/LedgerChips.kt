package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.ChargeDetails
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.EntryEdit
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.PaymentDetails
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_amount_paid
import net.neednot.renttracker.resources.ledger_cancelled_named
import net.neednot.renttracker.resources.ledger_cancelled_you
import net.neednot.renttracker.resources.ledger_custom_added_named
import net.neednot.renttracker.resources.ledger_custom_added_you
import net.neednot.renttracker.resources.ledger_date_by_named
import net.neednot.renttracker.resources.ledger_date_by_you
import net.neednot.renttracker.resources.ledger_edited_named
import net.neednot.renttracker.resources.ledger_edited_you
import net.neednot.renttracker.resources.ledger_late_fee_added_named
import net.neednot.renttracker.resources.ledger_late_fee_added_you
import net.neednot.renttracker.resources.ledger_late_fee_title
import net.neednot.renttracker.resources.ledger_payment_logged_named
import net.neednot.renttracker.resources.ledger_payment_logged_you
import net.neednot.renttracker.resources.ledger_payment_received_named
import net.neednot.renttracker.resources.ledger_payment_received_you
import net.neednot.renttracker.resources.ledger_payment_title
import net.neednot.renttracker.resources.ledger_rent_title

internal fun EntryStatus.chips(): List<EntryChip> =
    when (this) {
        EntryStatus.Active -> emptyList()
        is EntryStatus.Bounced -> listOf(EntryChip.Bounced)
        is EntryStatus.Cancelled -> listOf(EntryChip.Cancelled)
    }
