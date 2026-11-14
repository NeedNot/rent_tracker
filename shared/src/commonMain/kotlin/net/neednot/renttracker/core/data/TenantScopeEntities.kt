package net.neednot.renttracker.core.data

data class TenantScopeEntities(
    val rentTerms: List<RentTermEntity>,
    val payments: List<PaymentEntity>,
    val debts: List<DebtEntity>,
    val ledgerRows: List<LedgerRowEntity>,
    val accountNames: List<AccountNameEntity>,
    val memberships: List<MembershipEntity>,
)
