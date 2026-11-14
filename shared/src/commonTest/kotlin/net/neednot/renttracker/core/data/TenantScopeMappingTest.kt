package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.shared.TenantSummary
import net.neednot.renttracker.model.shared.TenantSummaryState
import net.neednot.renttracker.model.tenants.Debt
import net.neednot.renttracker.model.tenants.LedgerRow
import net.neednot.renttracker.model.tenants.Membership
import net.neednot.renttracker.model.tenants.MembershipDebts
import net.neednot.renttracker.model.tenants.MembershipMembers
import net.neednot.renttracker.model.tenants.MembershipPayments
import net.neednot.renttracker.model.tenants.MembershipRentTerms
import net.neednot.renttracker.model.tenants.MembershipSettings
import net.neednot.renttracker.model.tenants.MembershipTenants
import net.neednot.renttracker.model.tenants.Payment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.neednot.renttracker.model.tenants.DebtKind as ConvexDebtKind
import net.neednot.renttracker.model.tenants.LedgerRowKind as ConvexLedgerRowKind
import net.neednot.renttracker.model.tenants.MembershipRole as ConvexMembershipRole
import net.neednot.renttracker.model.tenants.PaymentMethod as ConvexPaymentMethod

class TenantScopeMappingTest {
    @Test
    fun aPaymentWithoutAMethodKeepsItNull() {
        assertNull(payment(method = null).toEntity().method)
    }

    @Test
    fun aPaymentKeepsItsMethodAndBouncedFields() {
        val entity = payment(method = ConvexPaymentMethod.BankTransfer, bouncedAt = 9, bouncedBy = "a2").toEntity()

        assertEquals(PaymentMethod.BankTransfer, entity.method)
        assertEquals(9L, entity.bouncedAt)
        assertEquals("a2", entity.bouncedBy)
    }

    @Test
    fun debtKindsMapToTheirEntityKinds() {
        assertEquals(DebtKind.LateFee, debt(ConvexDebtKind.LateFee).toEntity().kind)
        assertEquals(DebtKind.Custom, debt(ConvexDebtKind.Custom).toEntity().kind)
    }

    @Test
    fun aLateFeeDebtKeepsItsMonth() {
        assertEquals("2026-03", debt(ConvexDebtKind.LateFee, forMonth = "2026-03").toEntity().forMonth)
    }

    @Test
    fun aLedgerRowKeepsItsKeyKindAndBalances() {
        val entity =
            LedgerRow(
                amountCents = 100,
                balanceOwedCents = 50,
                date = "2026-03-01",
                entryId = "p1",
                key = "payment:p1",
                kind = ConvexLedgerRowKind.Payment,
                mayNotApply = true,
                position = 4,
                tenantId = "t1",
            ).toEntity()

        assertEquals(
            LedgerRowEntity(
                tenantId = "t1",
                key = "payment:p1",
                kind = LedgerRowKind.Payment,
                entryId = "p1",
                month = null,
                date = "2026-03-01",
                amountCents = 100,
                position = 4,
                balanceOwedCents = 50,
                balanceCreditCents = null,
                mayNotApply = true,
            ),
            entity,
        )
    }

    @Test
    fun aTenantSummaryKeepsItsLateFeeSuggestion() {
        val entity =
            TenantSummary(
                creditCents = 0,
                lateFeeSuggestionCents = 7_500,
                lateFeeSuggestionMonth = "2026-03",
                lateFeesOwedCents = 0,
                otherOwedCents = 0,
                rentOwedCents = 0,
                state = TenantSummaryState.Late,
                tenantId = "t1",
            ).toEntity()

        assertEquals("2026-03", entity.lateFeeSuggestionMonth)
        assertEquals(7_500L, entity.lateFeeSuggestionCents)
    }

    @Test
    fun aMembershipMapsRoleAndEveryPermissionLevel() {
        val entity =
            Membership(
                creationTime = 0.0,
                id = "m1",
                accountId = "a1",
                debts = MembershipDebts.AddEdit,
                editOthers = true,
                groupId = "g1",
                members = MembershipMembers.Read,
                notificationsOn = true,
                payments = MembershipPayments.Full,
                rentTerms = MembershipRentTerms.Read,
                role = ConvexMembershipRole.Member,
                settings = MembershipSettings.Read,
                tenants = MembershipTenants.AddEdit,
                version = 1,
            ).toEntity()

        assertEquals(MembershipRole.Member, entity.role)
        assertEquals(AccessLevel.AddEdit, entity.debts)
        assertEquals(AccessLevel.Read, entity.members)
        assertEquals(AccessLevel.Full, entity.payments)
        assertEquals(AccessLevel.Read, entity.rentTerms)
        assertEquals(SettingsAccessLevel.Read, entity.settings)
        assertEquals(AccessLevel.AddEdit, entity.tenants)
        assertEquals(true, entity.editOthers)
    }

    private fun payment(
        method: ConvexPaymentMethod?,
        bouncedAt: Long? = null,
        bouncedBy: String? = null,
    ) = Payment(
        creationTime = 1.0,
        id = "p1",
        amountCents = 100,
        bouncedAt = bouncedAt,
        bouncedBy = bouncedBy,
        clientId = "c1",
        dateReceived = "2026-03-01",
        loggedBy = "a1",
        method = method,
        tenantId = "t1",
        version = 1,
    )

    private fun debt(
        kind: ConvexDebtKind,
        forMonth: String? = null,
    ) = Debt(
        creationTime = 1.0,
        id = "d1",
        amountCents = 100,
        createdBy = "a1",
        date = "2026-03-10",
        forMonth = forMonth,
        kind = kind,
        tenantId = "t1",
        version = 1,
    )
}
