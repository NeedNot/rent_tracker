package net.neednot.renttracker.feature.home.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.ArchivedGroup
import net.neednot.renttracker.feature.home.domain.FakeHomeRepository
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.presentation.uistate.ArchivedGroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.ArchivedGroupsUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.archived_groups_summary
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.home_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ArchivedGroupsViewModelTest : MainDispatcherTest() {
    private val repository = FakeHomeRepository()
    private val viewModel by lazy { ArchivedGroupsViewModel(repository, TimeZone.of("America/Chicago")) }

    private val oldMill =
        ArchivedGroup(
            groupId = "g9",
            name = "Old Mill Road",
            totalOwedCents = 90_000,
            tenantCount = 3,
            archivedAt = Instant.parse("2026-01-05T15:00:00Z"),
        )

    @Test
    fun showsLoadingUntilTheFirstServerResult() =
        runTest {
            viewModel.uiState.test {
                assertEquals(ArchivedGroupsUiState.Loading, awaitItem())
            }
        }

    @Test
    fun showsTheEmptyStateWhenNoGroupIsArchived() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.archivedGroups.value = Synced(emptyList(), SyncStatus.Synced)

                assertEquals(ArchivedGroupsUiState.Empty, awaitItem())
            }
        }

    @Test
    fun aGroupShowsWhatItOwesItsTenantCountAndTheDayItWasArchivedInThePhonesZone() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.archivedGroups.value = Synced(listOf(oldMill), SyncStatus.Synced)

                val expected =
                    ArchivedGroupCardUiState(
                        groupId = "g9",
                        name = "Old Mill Road",
                        balance = UiText.Resource(Res.string.balance_owes, listOf("$900.00")),
                        summary = UiText.Plural(Res.plurals.archived_groups_summary, 3, listOf(3, "Jan 5, 2026")),
                    )
                assertEquals(ArchivedGroupsUiState.Content(listOf(expected)), awaitItem())
            }
        }

    @Test
    fun aGroupThatOwesNothingIsPaidUp() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.archivedGroups.value = Synced(listOf(oldMill.copy(totalOwedCents = 0)), SyncStatus.Synced)

                val content = awaitItem() as ArchivedGroupsUiState.Content
                assertEquals(UiText.Resource(Res.string.balance_paid_up), content.groups.single().balance)
            }
        }

    @Test
    fun aFailureWithNothingCachedShowsTheError() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.archivedGroups.value = Synced(emptyList(), SyncStatus.Failed(HomeError.Offline))

                assertEquals(ArchivedGroupsUiState.Error(UiText.Resource(Res.string.home_error_offline)), awaitItem())
            }
        }
}
