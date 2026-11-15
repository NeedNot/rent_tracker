package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.DuplicateMatchKind
import net.neednot.renttracker.core.data.PaymentMethod

fun pendingPayment(
    clientId: String = "c1",
    dateReceived: LocalDate = LocalDate(2026, 3, 10),
    amountCents: Long = 55_000,
    method: PaymentMethod? = PaymentMethod.Zelle,
    state: PendingState = PendingState.Waiting,
) = PendingPayment(
    clientId = clientId,
    tenantName = "Apt 3B",
    amountCents = amountCents,
    dateReceived = dateReceived,
    method = method,
    state = state,
)

fun duplicateMatch(
    kind: DuplicateMatchKind = DuplicateMatchKind.Payment,
    method: PaymentMethod? = PaymentMethod.Zelle,
) = PendingDuplicateMatch(
    kind = kind,
    date = LocalDate(2026, 3, 9),
    amountCents = 55_000,
    method = method,
    byName = "Sam Rivera",
)
