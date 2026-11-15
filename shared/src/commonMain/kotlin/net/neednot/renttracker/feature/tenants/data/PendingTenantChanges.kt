package net.neednot.renttracker.feature.tenants.data

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.AccountNameEntity
import net.neednot.renttracker.core.data.DebtEntity
import net.neednot.renttracker.core.data.MembershipEntity
import net.neednot.renttracker.core.data.PaymentEntity
import net.neednot.renttracker.core.data.TenantSummaryEntity
import net.neednot.renttracker.feature.tenants.domain.PendingBalanceChange
import net.neednot.renttracker.feature.tenants.domain.PendingNewEntry

internal data class PendingTenantChanges(
    val summary: TenantSummaryEntity?,
    val payments: List<PaymentEntity>,
    val debts: List<DebtEntity>,
    val accountNames: List<AccountNameEntity>,
    val caller: MembershipEntity?,
)

private const val PART_SEPARATOR = ','
private const val FIELD_SEPARATOR = ':'
private const val ENTRY_PART_FIELDS = 3
private const val PAYMENT_KIND = "payment"
private const val DEBT_KIND = "debt"

internal fun PendingTenantChanges.balanceChange(balanceCentsWhenLogged: Long?): PendingBalanceChange? {
    val now = summary?.balanceCents()
    return if (balanceCentsWhenLogged != null && now != null) {
        PendingBalanceChange(wasCents = balanceCentsWhenLogged, nowCents = now)
    } else {
        null
    }
}

internal fun PendingTenantChanges.newEntriesSince(tenantFingerprint: String): List<PendingNewEntry> {
    val known = knownEntryKeys(tenantFingerprint)
    val people = PersonResolver(accountNames, caller?.accountId)
    val newPayments =
        payments
            .filter { it.deletedAt == null && entryKey(PAYMENT_KIND, it.id) !in known }
            .map { payment ->
                PendingNewEntry.Payment(
                    date = LocalDate.parse(payment.dateReceived),
                    amountCents = payment.amountCents,
                    by = people.personOf(payment.loggedBy),
                )
            }
    val newCharges =
        debts
            .filter { it.deletedAt == null && entryKey(DEBT_KIND, it.id) !in known }
            .mapNotNull { debt ->
                debt.chargeKind()?.let { kind ->
                    PendingNewEntry.Charge(
                        date = LocalDate.parse(debt.date),
                        amountCents = debt.amountCents,
                        by = people.personOf(debt.createdBy),
                        kind = kind,
                    )
                }
            }
    return (newPayments + newCharges).sortedBy { it.date }
}

private fun knownEntryKeys(tenantFingerprint: String): Set<String> =
    tenantFingerprint
        .split(PART_SEPARATOR)
        .map { it.split(FIELD_SEPARATOR) }
        .filter { it.size == ENTRY_PART_FIELDS }
        .map { (kind, id) -> entryKey(kind, id) }
        .toSet()

private fun entryKey(
    kind: String,
    id: String,
): String = "$kind$FIELD_SEPARATOR$id"
