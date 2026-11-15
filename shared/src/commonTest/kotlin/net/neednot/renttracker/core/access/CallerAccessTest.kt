package net.neednot.renttracker.core.access

import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.data.SettingsAccessLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallerAccessTest {
    private val viewer = PermissionPreset.Viewer.permissions
    private val bookkeeper = PermissionPreset.Bookkeeper.permissions
    private val manager = PermissionPreset.Manager.permissions

    @Test
    fun aViewerIsViewOnlyAndCanChangeNothing() {
        val access = member(viewer)

        assertTrue(access.isViewOnly)
        assertFalse(access.isOwner)
        assertFalse(access.canManageMembers)
        assertFalse(access.canEditPermissions)
        assertFalse(access.canEditSettings)
        assertFalse(access.canMoveTenants)
    }

    @Test
    fun theOwnerCanDoEverythingAndIsNeverViewOnly() {
        val access = owner()

        assertFalse(access.isViewOnly)
        assertTrue(access.isOwner)
        assertTrue(access.canManageMembers)
        assertTrue(access.canEditPermissions)
        assertTrue(access.canEditSettings)
        assertTrue(access.canMoveTenants)
    }

    @Test
    fun anyWriteLevelOrFullSettingsEndsViewOnly() {
        assertFalse(member(bookkeeper).isViewOnly)
        assertFalse(member(viewer.copy(settings = SettingsAccessLevel.Full)).isViewOnly)
        assertFalse(member(viewer.copy(members = AccessLevel.AddEdit)).isViewOnly)
    }

    @Test
    fun theEditOthersSwitchAloneDoesNotEndViewOnly() {
        assertTrue(member(viewer.copy(editOthers = true)).isViewOnly)
    }

    @Test
    fun membersAddEditManagesMembersButOnlyFullEditsPermissions() {
        val addEdit = member(viewer.copy(members = AccessLevel.AddEdit))
        val full = member(viewer.copy(members = AccessLevel.Full))

        assertTrue(addEdit.canManageMembers)
        assertFalse(addEdit.canEditPermissions)
        assertTrue(full.canManageMembers)
        assertTrue(full.canEditPermissions)
    }

    @Test
    fun onlyTenantsFullMovesTenants() {
        assertFalse(member(manager).canMoveTenants)
        assertTrue(member(manager.copy(tenants = AccessLevel.Full)).canMoveTenants)
    }

    @Test
    fun anArchivedGroupAllowsNoChangesEvenToTheOwner() {
        val access = owner().copy(groupArchived = true)

        assertFalse(access.canManageMembers)
        assertFalse(access.canEditPermissions)
        assertFalse(access.canEditSettings)
        assertFalse(access.canMoveTenants)
        assertFalse(access.isViewOnly)
    }

    @Test
    fun anInviterIsOfferedLevelsAtOrBelowTheirOwn() {
        val access = member(manager.copy(members = AccessLevel.AddEdit))

        assertEquals(
            listOf(AccessLevel.Read, AccessLevel.AddEdit),
            access.grantableLevels(PermissionCategory.Payments),
        )
        assertEquals(listOf(SettingsAccessLevel.Read), access.grantableSettingsLevels())
    }

    @Test
    fun theOwnerIsOfferedEveryLevel() {
        val access = owner()

        assertEquals(AccessLevel.entries, access.grantableLevels(PermissionCategory.Tenants))
        assertEquals(SettingsAccessLevel.entries, access.grantableSettingsLevels())
    }

    @Test
    fun aLevelTheMemberAlreadyHasStaysSelectable() {
        val access = member(manager.copy(members = AccessLevel.Full))

        assertEquals(AccessLevel.entries, access.grantableLevels(PermissionCategory.Tenants, AccessLevel.Full))
        assertEquals(SettingsAccessLevel.entries, access.grantableSettingsLevels(SettingsAccessLevel.Full))
    }

    @Test
    fun theEditOthersSwitchIsOfferedToHoldersAndToFullPaymentsAndDebts() {
        val fullMoney = viewer.copy(payments = AccessLevel.Full, debts = AccessLevel.Full)

        assertTrue(member(manager).canGrantEditOthers())
        assertTrue(member(fullMoney).canGrantEditOthers())
        assertFalse(member(bookkeeper).canGrantEditOthers())
        assertTrue(member(bookkeeper).canGrantEditOthers(currentlyOn = true))
    }

    @Test
    fun anInviterMayGrantOnlyPresetsAtOrBelowTheirOwn() {
        val access = member(bookkeeper.copy(members = AccessLevel.AddEdit))

        assertEquals(listOf(PermissionPreset.Viewer, PermissionPreset.Bookkeeper), access.grantablePresets())
        assertEquals(PermissionPreset.entries, owner().grantablePresets())
    }

    @Test
    fun anEditorMayKeepWhatTheMemberHasWhileLoweringOtherLevels() {
        val access = member(bookkeeper.copy(members = AccessLevel.Full))
        val current = manager
        val lowered = manager.copy(rentTerms = AccessLevel.Read)

        assertTrue(access.mayGrant(lowered, current))
        assertFalse(access.mayGrant(lowered.copy(tenants = AccessLevel.Full), current))
        assertFalse(access.mayGrant(lowered))
    }

    @Test
    fun membersAddEditRemovesOnlyMembersAtOrBelowAndNeverTheOwnerOrThemselves() {
        val access = member(manager.copy(members = AccessLevel.AddEdit))

        assertTrue(access.canRemove(MembershipRole.Member, bookkeeper, accountId = "sam"))
        assertFalse(access.canRemove(MembershipRole.Member, manager.copy(debts = AccessLevel.Full), "sam"))
        assertFalse(access.canRemove(MembershipRole.Owner, viewer, accountId = "dana"))
        assertFalse(access.canRemove(MembershipRole.Member, viewer, accountId = ME))
    }

    @Test
    fun membersFullRemovesAnyoneButTheOwner() {
        val access = member(viewer.copy(members = AccessLevel.Full))

        assertTrue(access.canRemove(MembershipRole.Member, PermissionPreset.ownerPermissions, accountId = "sam"))
        assertFalse(access.canRemove(MembershipRole.Owner, viewer, accountId = "dana"))
    }

    @Test
    fun recognisesTheCallersOwnAccount() {
        assertTrue(member(viewer).isYou(ME))
        assertFalse(member(viewer).isYou("sam"))
    }

    private fun member(permissions: PermissionSet) =
        CallerAccess(
            membershipId = "m1",
            accountId = ME,
            role = MembershipRole.Member,
            permissions = permissions,
            groupArchived = false,
        )

    private fun owner() = member(PermissionPreset.ownerPermissions).copy(role = MembershipRole.Owner)

    private companion object {
        const val ME = "me"
    }
}
