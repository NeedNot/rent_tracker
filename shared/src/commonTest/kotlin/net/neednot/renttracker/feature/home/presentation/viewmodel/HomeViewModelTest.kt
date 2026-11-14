package net.neednot.renttracker.feature.home.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.FakeHomeRepository
import net.neednot.renttracker.feature.home.domain.GroupOverview
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.presentation.uistate.GroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.home_archived_owed
import net.neednot.renttracker.resources.home_error_offline
import net.neednot.renttracker.resources.home_error_unexpected
import net.neednot.renttracker.resources.home_late_count
import net.neednot.renttracker.resources.home_tenant_count
import net.neednot.renttracker.resources.home_tenant_count_none_late
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : MainDispatcherTest() {
    private val repository = FakeHomeRepository()
    private val viewModel by lazy { HomeViewModel(repository) }

    private val firstStreet =
        GroupOverview(
            groupId = "g1",
            name = "1st Street",
            totalOwedCents = 390_000,
            activeTenantCount = 6,
            lateTenantCount = 2,
            archivedOwedCents = 240_000,
        )

    private val mapleCourt =
        GroupOverview(
            groupId = "g2",
            name = "Maple Court",
            totalOwedCents = 0,
            activeTenantCount = 1,
            lateTenantCount = 0,
            archivedOwedCents = 0,
        )

    @Test
    fun showsLoadingUntilTheFirstServerResult() =
        runTest {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
            }
        }

    @Test
    fun showsTheEmptyStateWhenTheServerHasNoGroups() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.groups.value = Synced(emptyList(), SyncStatus.Synced)

                assertEquals(HomeUiState.Empty, awaitItem())
            }
        }

    @Test
    fun aGroupThatOwesShowsItsTotalLateCountAndArchivedDebt() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.groups.value = Synced(listOf(firstStreet), SyncStatus.Synced)

                val expected =
                    GroupCardUiState(
                        groupId = "g1",
                        name = "1st Street",
                        balance = UiText.Resource(Res.string.balance_owes, listOf("$3,900.00")),
                        isPaidUp = false,
                        tenantCount = UiText.Plural(Res.plurals.home_tenant_count, 6, listOf(6)),
                        lateCount = UiText.Resource(Res.string.home_late_count, listOf(2)),
                        archivedOwed = UiText.Resource(Res.string.home_archived_owed, listOf("$2,400.00")),
                    )
                assertEquals(HomeUiState.Content(listOf(expected)), awaitItem())
            }
        }

    @Test
    fun aPaidUpGroupShowsPaidUpNoneLateAndNoArchivedRow() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.groups.value = Synced(listOf(mapleCourt), SyncStatus.Synced)

                val expected =
                    GroupCardUiState(
                        groupId = "g2",
                        name = "Maple Court",
                        balance = UiText.Resource(Res.string.balance_paid_up),
                        isPaidUp = true,
                        tenantCount = UiText.Plural(Res.plurals.home_tenant_count_none_late, 1, listOf(1)),
                        lateCount = null,
                        archivedOwed = null,
                    )
                assertEquals(HomeUiState.Content(listOf(expected)), awaitItem())
            }
        }

    @Test
    fun cachedGroupsShowWhileTheServerIsStillAnswering() =
        runTest {
            repository.groups.value = Synced(listOf(mapleCourt), SyncStatus.Syncing)

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(1, (awaitItem() as HomeUiState.Content).groups.size)
            }
        }

    @Test
    fun aFailureWithNothingCachedShowsTheError() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.groups.value = Synced(emptyList(), SyncStatus.Failed(HomeError.Unexpected("boom")))

                assertEquals(HomeUiState.Error(UiText.Resource(Res.string.home_error_unexpected)), awaitItem())
            }
        }

    @Test
    fun aFailureKeepsCachedGroupsOnScreenWithTheError() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.groups.value = Synced(listOf(mapleCourt), SyncStatus.Failed(HomeError.Offline))

                val content = awaitItem() as HomeUiState.Content
                assertEquals(listOf("g2"), content.groups.map { it.groupId })
                assertEquals(UiText.Resource(Res.string.home_error_offline), content.syncError)
            }
        }

    @Test
    fun keepsObservingForFiveSecondsAfterTheScreenLeaves() =
        runTest {
            val screen = launch { viewModel.uiState.collect {} }
            runCurrent()
            assertEquals(1, repository.groups.subscriptionCount.value)

            screen.cancel()
            advanceTimeBy(4_999)
            assertEquals(1, repository.groups.subscriptionCount.value)

            advanceTimeBy(2)
            assertEquals(0, repository.groups.subscriptionCount.value)
        }
}
