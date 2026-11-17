package net.neednot.renttracker.feature.group.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.FakeGroupFreshnessRepository
import net.neednot.renttracker.feature.group.domain.FakeGroupRepository
import net.neednot.renttracker.feature.group.domain.groupDetails
import net.neednot.renttracker.feature.group.domain.groupTenant
import net.neednot.renttracker.feature.group.domain.memberAccess
import net.neednot.renttracker.feature.group.domain.ownerAccess
import net.neednot.renttracker.feature.group.presentation.uistate.GroupChromeUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupMenuItem
import net.neednot.renttracker.feature.group.presentation.uistate.GroupUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_tenant_archived
import net.neednot.renttracker.resources.group_tenant_contact_and_archived
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelChromeTest : MainDispatcherTest() {
    private val repository = FakeGroupRepository()
    private val freshnessRepository = FakeGroupFreshnessRepository()
    private val viewModel by lazy {
        GroupViewModel("g1", repository, freshnessRepository, RecordingMessageDispatcher(), TimeZone.UTC)
    }
    private val tenants = listOf(groupTenant("t1"))

    private suspend fun chromeOf(
        details: net.neednot.renttracker.feature.group.domain.GroupDetails,
    ): GroupChromeUiState {
        var chrome: GroupChromeUiState? = null
        viewModel.uiState.test {
            skipItems(1)
            repository.group.value = Synced(details, SyncStatus.Synced)
            chrome = (awaitItem() as GroupUiState.Content).chrome
        }
        return checkNotNull(chrome)
    }

    @Test
    fun theOwnerMenuListsMembersSettingsTransferAndArchive() =
        runTest {
            val chrome = chromeOf(groupDetails(activeTenants = tenants, access = ownerAccess()))

            assertEquals(
                listOf(
                    GroupMenuItem.Members,
                    GroupMenuItem.Settings,
                    GroupMenuItem.TransferOwnership,
                    GroupMenuItem.Archive,
                ),
                chrome.menuItems,
            )
        }

    @Test
    fun aMemberMenuListsMembersSettingsAndLeave() =
        runTest {
            val chrome = chromeOf(groupDetails(activeTenants = tenants, access = memberAccess()))

            assertEquals(listOf(GroupMenuItem.Members, GroupMenuItem.Settings, GroupMenuItem.Leave), chrome.menuItems)
        }

    @Test
    fun aViewOnlyMemberIsLabelledAndCannotAddTenants() =
        runTest {
            val access = memberAccess(PermissionPreset.Viewer.permissions)

            val chrome = chromeOf(groupDetails(activeTenants = tenants, access = access))

            assertEquals(true, chrome.isViewOnly)
            assertEquals(false, chrome.canAddTenants)
        }

    @Test
    fun aMemberWithAddEditOnTenantsCanAddTenants() =
        runTest {
            val chrome = chromeOf(groupDetails(activeTenants = tenants, access = memberAccess()))

            assertEquals(false, chrome.isViewOnly)
            assertEquals(true, chrome.canAddTenants)
        }

    @Test
    fun aMemberWhoOnlyReadsTenantsCannotAddThemEvenWithoutBeingViewOnly() =
        runTest {
            val permissions = PermissionPreset.Bookkeeper.permissions.copy(tenants = AccessLevel.Read)

            val chrome = chromeOf(groupDetails(activeTenants = tenants, access = memberAccess(permissions)))

            assertEquals(false, chrome.isViewOnly)
            assertEquals(false, chrome.canAddTenants)
        }

    @Test
    fun anArchivedGroupIsMarkedArchivedWithNoMenuAndNoAddTenant() =
        runTest {
            val chrome =
                chromeOf(
                    groupDetails(
                        activeTenants = tenants,
                        access = ownerAccess(groupArchived = true),
                        archivedAt = Instant.parse("2026-01-05T15:00:00Z"),
                    ),
                )

            assertTrue(chrome.isArchived)
            assertEquals(emptyList(), chrome.menuItems)
            assertEquals(false, chrome.canAddTenants)
        }

    @Test
    fun aGroupThatIsNotArchivedIsNotMarkedArchived() =
        runTest {
            assertFalse(chromeOf(groupDetails(activeTenants = tenants)).isArchived)
        }

    @Test
    fun tenantsOfAnArchivedGroupShowWhenTheGroupWasArchived() =
        runTest {
            val archivedTenants = listOf(groupTenant("t1", contactName = "Noor Aziz"), groupTenant("t2"))
            val details =
                groupDetails(
                    activeTenants = archivedTenants,
                    access = ownerAccess(groupArchived = true),
                    archivedAt = Instant.parse("2026-02-02T15:00:00Z"),
                )

            viewModel.uiState.test {
                skipItems(1)
                repository.group.value = Synced(details, SyncStatus.Synced)

                assertEquals(
                    listOf(
                        UiText.Resource(Res.string.group_tenant_contact_and_archived, listOf("Noor Aziz", "Feb 2")),
                        UiText.Resource(Res.string.group_tenant_archived, listOf("Feb 2")),
                    ),
                    (awaitItem() as GroupUiState.Content).tenants.map { it.detail },
                )
            }
        }
}
