package net.neednot.renttracker.feature.group.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.FakeGroupFreshnessRepository
import net.neednot.renttracker.feature.group.domain.FakeGroupRepository
import net.neednot.renttracker.feature.group.domain.groupDetails
import net.neednot.renttracker.feature.group.domain.groupTenant
import net.neednot.renttracker.feature.group.domain.ownerAccess
import net.neednot.renttracker.feature.group.presentation.uistate.GroupMenuItem
import net.neednot.renttracker.feature.group.presentation.uistate.GroupUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.offline_banner
import net.neednot.renttracker.resources.offline_banner_minutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class GroupOfflineViewModelTest : MainDispatcherTest() {
    private val repository = FakeGroupRepository()
    private val freshnessRepository = FakeGroupFreshnessRepository()
    private val viewModel by lazy {
        GroupViewModel("g1", repository, freshnessRepository, RecordingMessageDispatcher(), TimeZone.UTC)
    }
    private val tenants = listOf(groupTenant("t1"))

    private fun showGroup(
        archivedAt: Instant? = null,
        tenants: List<net.neednot.renttracker.feature.group.domain.GroupTenant> = this.tenants,
    ) {
        repository.group.value =
            Synced(
                groupDetails(
                    activeTenants = tenants,
                    access = ownerAccess(groupArchived = archivedAt != null),
                    archivedAt = archivedAt,
                ),
                SyncStatus.Synced,
            )
    }

    @Test
    fun offlineShowsTheBannerWithHowOldTheCacheIs() =
        runTest {
            showGroup()
            freshnessRepository.freshness.value = Freshness.Offline(15.minutes)

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(
                    UiText.Resource(Res.string.offline_banner_minutes, listOf(15L)),
                    awaitItem().offlineBanner,
                )
            }
        }

    @Test
    fun onlineShowsNoBanner() =
        runTest {
            showGroup()

            viewModel.uiState.test {
                skipItems(1)

                assertNull(awaitItem().offlineBanner)
            }
        }

    @Test
    fun offlineShowsTheBannerEvenBeforeTheGroupIsCached() =
        runTest {
            freshnessRepository.freshness.value = Freshness.Offline(null)

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(UiText.Resource(Res.string.offline_banner), awaitItem().offlineBanner)
            }
        }

    @Test
    fun offlineHidesTheGroupMenuAndTheAddTenantAction() =
        runTest {
            showGroup()
            freshnessRepository.freshness.value = Freshness.Offline(null)

            viewModel.uiState.test {
                skipItems(1)

                val chrome = (awaitItem() as GroupUiState.Content).chrome
                assertEquals(emptyList(), chrome.menuItems)
                assertEquals(false, chrome.canAddTenants)
            }
        }

    @Test
    fun offlineHidesTheAddTenantActionOnAGroupWithNoTenants() =
        runTest {
            showGroup(tenants = emptyList())
            freshnessRepository.freshness.value = Freshness.Offline(null)

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(false, (awaitItem() as GroupUiState.Empty).chrome.canAddTenants)
            }
        }

    @Test
    fun offlineHidesUnarchivingAnArchivedGroup() =
        runTest {
            showGroup(archivedAt = Instant.parse("2026-01-05T15:00:00Z"))
            freshnessRepository.freshness.value = Freshness.Offline(null)

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(false, (awaitItem() as GroupUiState.Content).chrome.canUnarchive)
            }
        }

    @Test
    fun onlineShowsTheMenuAndTheAddTenantAction() =
        runTest {
            showGroup()

            viewModel.uiState.test {
                skipItems(1)

                val chrome = (awaitItem() as GroupUiState.Content).chrome
                assertEquals(
                    listOf(GroupMenuItem.Members, GroupMenuItem.Settings, GroupMenuItem.Archive),
                    chrome.menuItems,
                )
                assertTrue(chrome.canAddTenants)
                assertTrue(chrome.canUnarchive)
            }
        }
}
