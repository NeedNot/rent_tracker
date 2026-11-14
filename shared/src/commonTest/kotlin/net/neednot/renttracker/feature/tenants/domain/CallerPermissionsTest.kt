package net.neednot.renttracker.feature.tenants.domain

import net.neednot.renttracker.core.data.AccessLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class CallerPermissionsTest {
    @Test
    fun logPaymentsNeedsAddAndEditOrMoreInPayments() {
        val cases =
            mapOf(
                AccessLevel.Read to false,
                AccessLevel.AddEdit to true,
                AccessLevel.Full to true,
            )

        cases.forEach { (level, expected) ->
            val permissions = callerPermissions(isOwner = false, payments = level, debts = AccessLevel.Read)
            assertEquals(expected, permissions.canLogPayments, "payments = $level")
            assertEquals(false, permissions.canAddCharges, "debts stay read-only")
        }
    }

    @Test
    fun addChargesNeedsAddAndEditOrMoreInDebts() {
        val cases =
            mapOf(
                AccessLevel.Read to false,
                AccessLevel.AddEdit to true,
                AccessLevel.Full to true,
            )

        cases.forEach { (level, expected) ->
            val permissions = callerPermissions(isOwner = false, payments = AccessLevel.Read, debts = level)
            assertEquals(expected, permissions.canAddCharges, "debts = $level")
        }
    }

    @Test
    fun anOwnerCanDoEverythingWhateverTheLevels() {
        val owner = callerPermissions(isOwner = true, payments = AccessLevel.Read, debts = AccessLevel.Read)

        assertEquals(true, owner.canLogPayments)
        assertEquals(true, owner.canAddCharges)
        assertEquals(true, owner.canChangePayment(loggedBy = "someone"))
        assertEquals(true, owner.canChangeCharge(addedBy = "someone"))
    }

    @Test
    fun fullAccessChangesEntriesAddedByOthers() {
        val full = callerPermissions(isOwner = false, payments = AccessLevel.Full, debts = AccessLevel.Full)

        assertEquals(true, full.canChangePayment(loggedBy = "someone"))
        assertEquals(true, full.canChangeCharge(addedBy = "someone"))
    }

    @Test
    fun addAndEditChangesOnlyOwnEntriesUnlessEditOthersIsOn() {
        val own = callerPermissions(isOwner = false, payments = AccessLevel.AddEdit, debts = AccessLevel.AddEdit)
        val withEditOthers = own.copy(editOthers = true)

        assertEquals(true, own.canChangePayment(loggedBy = "me"))
        assertEquals(true, own.canChangeCharge(addedBy = "me"))
        assertEquals(false, own.canChangePayment(loggedBy = "someone"))
        assertEquals(false, own.canChangeCharge(addedBy = "someone"))
        assertEquals(true, withEditOthers.canChangePayment(loggedBy = "someone"))
        assertEquals(true, withEditOthers.canChangeCharge(addedBy = "someone"))
    }

    @Test
    fun readOnlyChangesNothingEvenWithEditOthers() {
        val readOnly =
            callerPermissions(isOwner = false, payments = AccessLevel.Read, debts = AccessLevel.Read, editOthers = true)

        assertEquals(false, readOnly.canChangePayment(loggedBy = "me"))
        assertEquals(false, readOnly.canChangeCharge(addedBy = "me"))
    }

    @Test
    fun theCategoriesAreIndependent() {
        val permissions = callerPermissions(isOwner = false, payments = AccessLevel.Full, debts = AccessLevel.Read)

        assertEquals(true, permissions.canChangePayment(loggedBy = "someone"))
        assertEquals(false, permissions.canChangeCharge(addedBy = "someone"))
    }
}
