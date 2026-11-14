package net.neednot.renttracker.feature.group.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.FakeGroupRepository
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.TenantBalance
import net.neednot.renttracker.feature.group.domain.groupDetails
import net.neednot.renttracker.feature.group.domain.groupTenant
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantRowUiState
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantsUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_credit
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.group_error_not_found
import net.neednot.renttracker.resources.group_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchivedTenantsViewModelTest : MainDispatcherTest() {
    private val repository = FakeGroupRepository()
    private val messageDispatcher = RecordingMessageDispatcher()
    private val viewModel by lazy { ArchivedTenantsViewModel("g1", repository, messageDispatcher) }

    private val archivedTenants =
        listOf(
            groupTenant(
                tenantId = "t7",
                name = "Apt 4C",
                contactName = "Sam Park",
                balance = TenantBalance.Owes(240_000),
            ),
            groupTenant(tenantId = "t8", name = "Apt 5A", balance = TenantBalance.Credit(5_000)),
            groupTenant(tenantId = "t9", name = "Apt 6B"),
        )

    @Test
    fun showsLoadingUntilTheGroupArrives() =
        runTest {
            viewModel.uiState.test {
                assertEquals(ArchivedTenantsUiState.Loading, awaitItem())
            }
        }

    @Test
    fun archivedTenantsShowTheirRemainingBalanceInWords() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value = Synced(groupDetails(archivedTenants = archivedTenants), SyncStatus.Synced)

                val expected =
                    listOf(
                        ArchivedTenantRowUiState(
                            tenantId = "t7",
                            name = "Apt 4C",
                            contactName = "Sam Park",
                            balance = UiText.Resource(Res.string.balance_owes, listOf("$2,400.00")),
                        ),
                        ArchivedTenantRowUiState(
                            tenantId = "t8",
                            name = "Apt 5A",
                            contactName = null,
                            balance = UiText.Resource(Res.string.balance_credit, listOf("$50.00")),
                        ),
                        ArchivedTenantRowUiState(
                            tenantId = "t9",
                            name = "Apt 6B",
                            contactName = null,
                            balance = UiText.Resource(Res.string.balance_paid_up),
                        ),
                    )
                assertEquals(ArchivedTenantsUiState.Content(expected), awaitItem())
            }
        }

    @Test
    fun activeTenantsAreNotListed() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value =
                    Synced(groupDetails(activeTenants = listOf(groupTenant("t1"))), SyncStatus.Synced)

                assertEquals(ArchivedTenantsUiState.Empty, awaitItem())
            }
        }

    @Test
    fun aFailureKeepsCachedTenantsOnScreenWithTheError() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value =
                    Synced(groupDetails(archivedTenants = archivedTenants), SyncStatus.Failed(GroupError.Offline))

                val content = awaitItem() as ArchivedTenantsUiState.Content
                assertEquals(listOf("t7", "t8", "t9"), content.tenants.map { it.tenantId })
                assertEquals(UiText.Resource(Res.string.group_error_offline), content.syncError)
            }
        }

    @Test
    fun aGroupThatIsGoneShowsAMessageAndLeaves() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.events.test {
                repository.group.value = Synced(null, SyncStatus.Failed(GroupError.NotFound))

                assertEquals(GroupEvent.GroupGone, awaitItem())
            }
            assertEquals(
                listOf(UiMessage(UiText.Resource(Res.string.group_error_not_found), UiMessageVariant.Error)),
                messageDispatcher.shown,
            )
        }
}
