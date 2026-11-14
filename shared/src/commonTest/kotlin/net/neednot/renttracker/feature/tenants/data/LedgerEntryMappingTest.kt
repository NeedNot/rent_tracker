package net.neednot.renttracker.feature.tenants.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.AccountNameEntity
import net.neednot.renttracker.core.data.DebtEntity
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.data.LedgerRowEntity
import net.neednot.renttracker.core.data.LedgerRowKind
import net.neednot.renttracker.core.data.PaymentEntity
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.data.accountNameEntity
import net.neednot.renttracker.core.data.debtEntity
import net.neednot.renttracker.core.data.ledgerRowEntity
import net.neednot.renttracker.core.data.paymentEntity
import net.neednot.renttracker.feature.tenants.domain.CallerPermissions
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.EntryEdit
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.LedgerBalance
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.domain.callerPermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class LedgerEntryMappingTest {
    private val owner = callerPermissions(accountId = "me", isOwner = true)

    @Test
    fun aRentRowBecomesARentEntryForItsMonth() {
        val row = ledgerRow("rent:2026-03", LedgerRowKind.Rent).copy(month = "2026-03", balanceOwedCents = 205_000)

        val entry = entriesOf(rows = listOf(row)).single() as LedgerEntry.Rent

        assertEquals(YearMonth(2026, 3), entry.month)
        assertEquals(LedgerBalance.Owed(205_000), entry.balance)
        assertEquals(150_000, entry.amountCents)
    }

    @Test
    fun theRunningBalanceIsOwedCreditPaidUpOrNone() {
        val cases =
            listOf(
                ledgerRow("a", LedgerRowKind.Rent).copy(month = "2026-03", balanceOwedCents = 1_000) to
                    LedgerBalance.Owed(1_000),
                ledgerRow("b", LedgerRowKind.Rent).copy(month = "2026-03", balanceCreditCents = 600) to
                    LedgerBalance.Credit(600),
                ledgerRow(
                    "c",
                    LedgerRowKind.Rent,
                ).copy(month = "2026-03", balanceOwedCents = 0) to LedgerBalance.PaidUp,
                ledgerRow("d", LedgerRowKind.Rent).copy(month = "2026-03") to null,
            )

        cases.forEach { (row, expected) ->
            assertEquals(expected, (entriesOf(rows = listOf(row)).single()).balance, "row = ${row.key}")
        }
    }

    @Test
    fun entriesKeepTheOrderOfTheRows() {
        val rows =
            listOf(
                ledgerRow("rent:2026-03", LedgerRowKind.Rent).copy(month = "2026-03"),
                ledgerRow("rent:2026-02", LedgerRowKind.Rent).copy(month = "2026-02"),
            )

        assertEquals(listOf("rent:2026-03", "rent:2026-02"), entriesOf(rows = rows).map { it.key })
    }

    @Test
    fun aPaymentRowJoinsItsPaymentWithMethodDatesAndWhoLoggedIt() {
        val payment =
            paymentEntity("p1", "t1").copy(
                method = PaymentMethod.Zelle,
                note = "March rent",
                dateReceived = "2026-01-18",
                creationTime = Instant.parse("2026-01-24T18:00:00Z").toEpochMilliseconds(),
                loggedBy = "sam",
                version = 4,
            )
        val row = paymentRow("payment:p1", "p1").copy(date = "2026-01-18")

        val entry =
            entriesOf(
                rows = listOf(row),
                payments = listOf(payment),
                names = listOf(accountNameEntity("sam", "Sam Rivera")),
            ).single() as LedgerEntry.Payment

        assertEquals("p1", entry.payment.paymentId)
        assertEquals(4, entry.payment.version)
        assertEquals(PaymentMethod.Zelle, entry.payment.method)
        assertEquals("March rent", entry.payment.note)
        assertEquals(LocalDate(2026, 1, 18), entry.payment.dateReceived)
        assertEquals(Instant.parse("2026-01-24T18:00:00Z"), entry.payment.loggedAt)
        assertEquals(Person.Named("Sam Rivera"), entry.payment.loggedBy)
        assertEquals(EntryStatus.Active, entry.payment.status)
    }

    @Test
    fun theCallersOwnAccountIsYouAndAMissingNameIsADeletedUser() {
        val mine = paymentEntry("p1").copy(loggedBy = "me")
        val gone = paymentEntry("p2").copy(loggedBy = "ghost")
        val rows = listOf(paymentRow("payment:p1", "p1"), paymentRow("payment:p2", "p2"))

        val entries = entriesOf(rows = rows, payments = listOf(mine, gone)).map { it as LedgerEntry.Payment }

        assertEquals(Person.You, entries[0].payment.loggedBy)
        assertEquals(Person.Named("Deleted user"), entries[1].payment.loggedBy)
    }

    @Test
    fun aCancelledPaymentCarriesTheReasonWhoAndWhen() {
        val payment =
            paymentEntry("p1").copy(
                deletedAt = Instant.parse("2026-03-05T12:00:00Z").toEpochMilliseconds(),
                deletedBy = "sam",
                deleteReason = "Entered twice",
            )

        val entry =
            entriesOf(
                rows = listOf(paymentRow("payment:p1", "p1")),
                payments = listOf(payment),
                names = listOf(accountNameEntity("sam", "Sam Rivera")),
            ).single() as LedgerEntry.Payment

        assertEquals(
            EntryStatus.Cancelled(
                reason = "Entered twice",
                by = Person.Named("Sam Rivera"),
                at = Instant.parse("2026-03-05T12:00:00Z"),
            ),
            entry.payment.status,
        )
    }

    @Test
    fun aBouncedPaymentCarriesWhenAndWho() {
        val payment =
            paymentEntry("p1").copy(
                bouncedAt = Instant.parse("2026-02-19T12:00:00Z").toEpochMilliseconds(),
                bouncedBy = "me",
            )

        val entry = entriesOf(rows = listOf(paymentRow("payment:p1", "p1")), payments = listOf(payment)).single()

        assertEquals(
            EntryStatus.Bounced(by = Person.You, at = Instant.parse("2026-02-19T12:00:00Z")),
            (entry as LedgerEntry.Payment).payment.status,
        )
    }

    @Test
    fun aPaymentBothCancelledAndBouncedShowsAsCancelled() {
        val payment =
            paymentEntry(
                "p1",
            ).copy(bouncedAt = 1, bouncedBy = "me", deletedAt = 2, deletedBy = "me", deleteReason = "Oops")

        val entry = entriesOf(rows = listOf(paymentRow("payment:p1", "p1")), payments = listOf(payment)).single()

        assertEquals(true, (entry as LedgerEntry.Payment).payment.status is EntryStatus.Cancelled)
    }

    @Test
    fun anEditedEntryShowsWhoEditedAndWhen() {
        val payment =
            paymentEntry("p1").copy(
                lastEditedAt = Instant.parse("2026-03-04T12:00:00Z").toEpochMilliseconds(),
                lastEditedBy = "me",
            )

        val entry = entriesOf(rows = listOf(paymentRow("payment:p1", "p1")), payments = listOf(payment)).single()

        assertEquals(
            EntryEdit(by = Person.You, at = Instant.parse("2026-03-04T12:00:00Z")),
            (entry as LedgerEntry.Payment).payment.edit,
        )
    }

    @Test
    fun aLateFeeDebtIsALateFeeForItsMonthAndMayNotApplyComesFromTheRow() {
        val debt =
            debtEntry("d1").copy(kind = DebtKind.LateFee, forMonth = "2026-01", createdBy = "sam", amountCents = 7_000)
        val row = debtRow("debt:d1", "d1").copy(mayNotApply = true)

        val entry =
            entriesOf(rows = listOf(row), debts = listOf(debt), names = listOf(accountNameEntity("sam", "Sam Rivera")))
                .single() as LedgerEntry.Charge

        assertEquals(ChargeKind.LateFee(YearMonth(2026, 1)), entry.charge.kind)
        assertEquals(true, entry.charge.mayNotApply)
        assertEquals(Person.Named("Sam Rivera"), entry.charge.addedBy)
    }

    @Test
    fun aCustomDebtIsNamedByItsNote() {
        val debt = debtEntry("d1").copy(kind = DebtKind.Custom, note = "Water bill, Feb")

        val entry =
            entriesOf(
                rows = listOf(debtRow("debt:d1", "d1")),
                debts = listOf(debt),
            ).single() as LedgerEntry.Charge

        assertEquals(ChargeKind.Custom("Water bill, Feb"), entry.charge.kind)
    }

    @Test
    fun aCancelledDebtCarriesTheReason() {
        val debt = debtEntry("d1").copy(deletedAt = 5, deletedBy = "me", deleteReason = "Wrong tenant")

        val entry =
            entriesOf(
                rows = listOf(debtRow("debt:d1", "d1")),
                debts = listOf(debt),
            ).single() as LedgerEntry.Charge

        assertEquals(
            EntryStatus.Cancelled("Wrong tenant", Person.You, Instant.fromEpochMilliseconds(5)),
            entry.charge.status,
        )
    }

    @Test
    fun aRowWhoseEntryIsNotStoredIsLeftOut() {
        val rows = listOf(paymentRow("payment:gone", "gone"), debtRow("debt:gone", "gone"))

        assertEquals(emptyList(), entriesOf(rows = rows))
    }

    @Test
    fun anArchivedTenantsEntriesCannotBeChanged() {
        val rows = listOf(paymentRow("payment:p1", "p1"), debtRow("debt:d1", "d1"))
        val sources = sources(rows = rows, payments = listOf(paymentEntry("p1")), debts = listOf(debtEntry("d1")))

        val entries = sources.toLedgerEntries(owner, isArchived = true)

        assertEquals(listOf(false, false), entries.map { it.canChange() })
    }

    @Test
    fun entriesCanBeChangedFollowingThePermissionRules() {
        val cases =
            listOf(
                ChangeCase("owner on others", callerPermissions(accountId = "me", isOwner = true), "sam", true),
                ChangeCase("full on others", member(AccessLevel.Full, editOthers = false), "sam", true),
                ChangeCase("add and edit on own", member(AccessLevel.AddEdit, editOthers = false), "me", true),
                ChangeCase("add and edit on others", member(AccessLevel.AddEdit, editOthers = false), "sam", false),
                ChangeCase(
                    "add and edit with edit others on others",
                    member(AccessLevel.AddEdit, editOthers = true),
                    "sam",
                    true,
                ),
                ChangeCase("read on own", member(AccessLevel.Read, editOthers = true), "me", false),
            )

        cases.forEach { case ->
            val sources =
                sources(
                    rows = listOf(paymentRow("payment:p1", "p1"), debtRow("debt:d1", "d1")),
                    payments = listOf(paymentEntry("p1").copy(loggedBy = case.addedBy)),
                    debts = listOf(debtEntry("d1").copy(createdBy = case.addedBy)),
                )

            val changeable = sources.toLedgerEntries(case.permissions, isArchived = false).map { it.canChange() }

            assertEquals(listOf(case.expected, case.expected), changeable, case.label)
        }
    }

    @Test
    fun withoutAMembershipNothingCanBeChanged() {
        val sources = sources(rows = listOf(paymentRow("payment:p1", "p1")), payments = listOf(paymentEntry("p1")))

        assertEquals(
            listOf(false),
            sources.toLedgerEntries(permissions = null, isArchived = false).map { it.canChange() },
        )
    }

    private class ChangeCase(
        val label: String,
        val permissions: CallerPermissions,
        val addedBy: String,
        val expected: Boolean,
    )

    private fun member(
        level: AccessLevel,
        editOthers: Boolean,
    ): CallerPermissions =
        callerPermissions(accountId = "me", isOwner = false, payments = level, debts = level, editOthers = editOthers)

    private fun LedgerEntry.canChange(): Boolean =
        when (this) {
            is LedgerEntry.Payment -> payment.canChange
            is LedgerEntry.Charge -> charge.canChange
            is LedgerEntry.Rent -> false
        }

    private fun entriesOf(
        rows: List<LedgerRowEntity>,
        payments: List<PaymentEntity> = emptyList(),
        debts: List<DebtEntity> = emptyList(),
        names: List<AccountNameEntity> = emptyList(),
    ): List<LedgerEntry> = sources(rows, payments, debts, names).toLedgerEntries(owner, isArchived = false)

    private fun sources(
        rows: List<LedgerRowEntity>,
        payments: List<PaymentEntity> = emptyList(),
        debts: List<DebtEntity> = emptyList(),
        names: List<AccountNameEntity> = emptyList(),
    ) = LedgerSources(
        rows = rows,
        payments = payments,
        debts = debts,
        accountNames = names,
        membership = null,
        dismissedMonths = emptyList(),
    )

    private fun ledgerRow(
        key: String,
        kind: LedgerRowKind,
    ) = ledgerRowEntity("t1", key, position = 0, kind = kind)

    private fun paymentRow(
        key: String,
        paymentId: String,
    ) = ledgerRow(key, LedgerRowKind.Payment).copy(entryId = paymentId)

    private fun debtRow(
        key: String,
        debtId: String,
    ) = ledgerRow(key, LedgerRowKind.Debt).copy(entryId = debtId)

    private fun paymentEntry(id: String) = paymentEntity(id, "t1").copy(loggedBy = "me")

    private fun debtEntry(id: String) = debtEntity(id, "t1").copy(createdBy = "me")
}
