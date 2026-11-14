package net.neednot.renttracker.feature.tenants.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import net.neednot.renttracker.core.data.AccountNameEntity
import net.neednot.renttracker.core.data.DebtEntity
import net.neednot.renttracker.core.data.LedgerRowEntity
import net.neednot.renttracker.core.data.MembershipEntity
import net.neednot.renttracker.core.data.PaymentEntity
import net.neednot.renttracker.core.data.RentTrackerDatabase

internal data class LedgerSources(
    val rows: List<LedgerRowEntity>,
    val payments: List<PaymentEntity>,
    val debts: List<DebtEntity>,
    val accountNames: List<AccountNameEntity>,
    val membership: MembershipEntity?,
    val dismissedMonths: List<String>,
)

private data class LedgerEntries(
    val rows: List<LedgerRowEntity>,
    val payments: List<PaymentEntity>,
    val debts: List<DebtEntity>,
)

private data class LedgerPeople(
    val accountNames: List<AccountNameEntity>,
    val membership: MembershipEntity?,
    val dismissedMonths: List<String>,
)

internal fun RentTrackerDatabase.observeLedgerSources(
    tenantId: String,
    groupId: String,
): Flow<LedgerSources> =
    combine(observeLedgerEntries(tenantId), observeLedgerPeople(tenantId, groupId)) { entries, people ->
        LedgerSources(
            rows = entries.rows,
            payments = entries.payments,
            debts = entries.debts,
            accountNames = people.accountNames,
            membership = people.membership,
            dismissedMonths = people.dismissedMonths,
        )
    }

private fun RentTrackerDatabase.observeLedgerEntries(tenantId: String): Flow<LedgerEntries> =
    combine(
        ledgerRowDao().observeByTenantId(tenantId),
        paymentDao().observeByTenantId(tenantId),
        debtDao().observeByTenantId(tenantId),
        ::LedgerEntries,
    )

private fun RentTrackerDatabase.observeLedgerPeople(
    tenantId: String,
    groupId: String,
): Flow<LedgerPeople> =
    combine(
        accountNameDao().observeAll(),
        membershipDao().observeByGroupId(groupId),
        dismissedLateFeeSuggestionDao().observeDismissedMonths(tenantId),
        ::LedgerPeople,
    )
