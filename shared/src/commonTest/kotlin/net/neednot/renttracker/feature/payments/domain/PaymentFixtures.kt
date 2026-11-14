package net.neednot.renttracker.feature.payments.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

fun paymentOverview(
    balance: PaymentBalance? = PaymentBalance(owedCents = 55_000, creditCents = 0),
    today: LocalDate = LocalDate(2026, 3, 10),
    lastPaymentMethod: PaymentMethod? = PaymentMethod.Zelle,
    lateFeeSuggestion: LateFeeSuggestion? = LateFeeSuggestion(YearMonth(2026, 3), 5_000),
    canAddDebts: Boolean = true,
    callerAccountId: String? = "account",
    payment: SavedPayment? = null,
) = PaymentEntryOverview(
    tenantName = "Apt 3B",
    groupName = "1st Street",
    balance = balance,
    today = today,
    lastPaymentMethod = lastPaymentMethod,
    lateFeeSuggestion = lateFeeSuggestion,
    canAddDebts = canAddDebts,
    callerAccountId = callerAccountId,
    payment = payment,
)

fun savedPayment(
    version: Long = 2,
    amountCents: Long = 150_000,
    dateReceived: LocalDate = LocalDate(2026, 3, 2),
    method: PaymentMethod? = PaymentMethod.Check,
    note: String? = "March rent",
) = SavedPayment(
    paymentId = "p1",
    version = version,
    details = PaymentDraft(amountCents, dateReceived, method, note),
)

fun synced(overview: PaymentEntryOverview?): Synced<PaymentEntryOverview?, PaymentError> =
    Synced(overview, SyncStatus.Synced)
