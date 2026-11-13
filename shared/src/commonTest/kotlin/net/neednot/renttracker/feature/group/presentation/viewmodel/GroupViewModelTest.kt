package net.neednot.renttracker.feature.group.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.designsystem.components.TenantChipState
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.FakeGroupRepository
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.GroupTenantState
import net.neednot.renttracker.feature.group.domain.TenantBalance
import net.neednot.renttracker.feature.group.domain.groupDetails
import net.neednot.renttracker.feature.group.domain.groupTenant
import net.neednot.renttracker.feature.group.presentation.uistate.GroupUiState
import net.neednot.renttracker.feature.group.presentation.uistate.TenantRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_credit
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.group_archived
import net.neednot.renttracker.resources.group_archived_owed
import net.neednot.renttracker.resources.group_error_not_found
import net.neednot.renttracker.resources.group_error_offline
import net.neednot.renttracker.resources.group_error_unexpected
import net.neednot.renttracker.resources.group_tenant_contact_and_due
import net.neednot.renttracker.resources.group_tenant_count_late
import net.neednot.renttracker.resources.group_tenant_count_none_late
import net.neednot.renttracker.resources.group_tenant_due
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest : MainDispatcherTest() {
    private val repository = FakeGroupRepository()
    private val messageDispatcher = RecordingMessageDispatcher()
    private val viewModel by lazy { GroupViewModel("g1", repository, messageDispatcher) }

    private val lateTenant =
        groupTenant(
            tenantId = "t1",
            name = "Apt 1A",
            contactName = "Marcus Lee",
            nextDueDate = LocalDate(2026, 4, 1),
            state = GroupTenantState.Late,
            balance = TenantBalance.Owes(165_000),
        )
    private val paidUpTenant = groupTenant(tenantId = "t2", name = "Apt 1B")
    private val archivedTenant = groupTenant(tenantId = "t9", balance = TenantBalance.Owes(240_000))

    @Test
    fun showsLoadingUntilTheGroupArrives() =
        runTest {
            viewModel.uiState.test {
                assertEquals(GroupUiState.Loading, awaitItem())
            }
            assertEquals(listOf("g1"), repository.observedGroupIds)
        }

    @Test
    fun aGroupThatOwesShowsItsTotalTenantCountAndLateCount() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value =
                    Synced(
                        groupDetails(
                            activeTenants = listOf(lateTenant, paidUpTenant),
                            totalOwedCents = 165_000,
                            lateTenantCount = 1,
                        ),
                        SyncStatus.Synced,
                    )

                val content = awaitItem() as GroupUiState.Content
                assertEquals("1st Street", content.groupName)
                assertEquals(UiText.Resource(Res.string.balance_owes, listOf("$1,650.00")), content.balance)
                assertEquals(UiText.Plural(Res.plurals.group_tenant_count_late, 2, listOf(2, 1)), content.tenantCounts)
            }
        }

    @Test
    fun aGroupThatOwesNothingShowsPaidUpAndNoneLate() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value = Synced(groupDetails(activeTenants = listOf(paidUpTenant)), SyncStatus.Synced)

                val content = awaitItem() as GroupUiState.Content
                assertEquals(UiText.Resource(Res.string.balance_paid_up), content.balance)
                assertEquals(
                    UiText.Plural(Res.plurals.group_tenant_count_none_late, 1, listOf(1)),
                    content.tenantCounts,
                )
            }
        }

    @Test
    fun aTenantRowShowsContactDueDateBalanceInWordsAndState() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value = Synced(groupDetails(activeTenants = listOf(lateTenant)), SyncStatus.Synced)

                val expected =
                    TenantRowUiState(
                        tenantId = "t1",
                        name = "Apt 1A",
                        detail =
                            UiText.Resource(
                                Res.string.group_tenant_contact_and_due,
                                listOf("Marcus Lee", "Apr 1"),
                            ),
                        balance = UiText.Resource(Res.string.balance_owes, listOf("$1,650.00")),
                        chipState = TenantChipState.Late,
                    )
                assertEquals(listOf(expected), (awaitItem() as GroupUiState.Content).tenants)
            }
        }

    @Test
    fun aPaidUpTenantWithoutContactOrDueDateShowsOnlyItsState() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value = Synced(groupDetails(activeTenants = listOf(paidUpTenant)), SyncStatus.Synced)

                val expected =
                    TenantRowUiState(
                        tenantId = "t2",
                        name = "Apt 1B",
                        detail = null,
                        balance = null,
                        chipState = TenantChipState.PaidUp,
                    )
                assertEquals(listOf(expected), (awaitItem() as GroupUiState.Content).tenants)
            }
        }

    @Test
    fun aTenantInCreditShowsTheCreditAndADueDateWithoutContact() =
        runTest {
            val inCredit =
                groupTenant(
                    tenantId = "t3",
                    nextDueDate = LocalDate(2026, 4, 15),
                    balance = TenantBalance.Credit(40_000),
                )

            viewModel.uiState.test {
                skipItems(1)

                repository.group.value = Synced(groupDetails(activeTenants = listOf(inCredit)), SyncStatus.Synced)

                val row = (awaitItem() as GroupUiState.Content).tenants.single()
                assertEquals(UiText.Resource(Res.string.balance_credit, listOf("$400.00")), row.balance)
                assertEquals(UiText.Resource(Res.string.group_tenant_due, listOf("Apr 15")), row.detail)
                assertEquals(TenantChipState.PaidUp, row.chipState)
            }
        }

    @Test
    fun theArchivedRowIsHiddenWithoutArchivedTenants() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value = Synced(groupDetails(activeTenants = listOf(paidUpTenant)), SyncStatus.Synced)

                assertEquals(null, (awaitItem() as GroupUiState.Content).archived)
            }
        }

    @Test
    fun theArchivedRowShowsTheCountAndWhatArchivedTenantsOwe() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value =
                    Synced(
                        groupDetails(
                            activeTenants = listOf(paidUpTenant),
                            archivedTenants = listOf(archivedTenant),
                            archivedOwedCents = 240_000,
                        ),
                        SyncStatus.Synced,
                    )

                assertEquals(
                    UiText.Resource(Res.string.group_archived_owed, listOf(1, "$2,400.00")),
                    (awaitItem() as GroupUiState.Content).archived,
                )
            }
        }

    @Test
    fun theArchivedRowOmitsTheDebtWhenArchivedTenantsOweNothing() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value =
                    Synced(
                        groupDetails(activeTenants = listOf(paidUpTenant), archivedTenants = listOf(groupTenant("t8"))),
                        SyncStatus.Synced,
                    )

                assertEquals(
                    UiText.Resource(Res.string.group_archived, listOf(1)),
                    (awaitItem() as GroupUiState.Content).archived,
                )
            }
        }

    @Test
    fun aGroupWithoutActiveTenantsShowsTheEmptyStateWithItsArchivedRow() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value =
                    Synced(groupDetails(archivedTenants = listOf(groupTenant("t8"))), SyncStatus.Synced)

                assertEquals(
                    GroupUiState.Empty(
                        groupName = "1st Street",
                        archived = UiText.Resource(Res.string.group_archived, listOf(1)),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun aCachedGroupWithoutTenantsKeepsLoadingUntilTheServerAnswers() =
        runTest {
            repository.group.value = Synced(groupDetails(), SyncStatus.Syncing)

            viewModel.uiState.test {
                assertEquals(GroupUiState.Loading, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun aFailureWithNothingCachedShowsTheError() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value = Synced(null, SyncStatus.Failed(GroupError.Unexpected("boom")))

                assertEquals(GroupUiState.Error(UiText.Resource(Res.string.group_error_unexpected)), awaitItem())
            }
        }

    @Test
    fun aFailureKeepsCachedTenantsOnScreenWithTheError() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.group.value =
                    Synced(groupDetails(activeTenants = listOf(paidUpTenant)), SyncStatus.Failed(GroupError.Offline))

                val content = awaitItem() as GroupUiState.Content
                assertEquals(listOf("t2"), content.tenants.map { it.tenantId })
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

    @Test
    fun otherFailuresShowNoMessageAndStayOnTheGroup() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.events.test {
                repository.group.value = Synced(null, SyncStatus.Failed(GroupError.Offline))
                runCurrent()

                expectNoEvents()
            }
            assertTrue(messageDispatcher.shown.isEmpty())
        }

    @Test
    fun keepsObservingForFiveSecondsAfterTheScreenLeaves() =
        runTest {
            val screen = launch { viewModel.uiState.collect {} }
            runCurrent()
            assertEquals(1, repository.group.subscriptionCount.value)

            screen.cancel()
            advanceTimeBy(4_999)
            assertEquals(1, repository.group.subscriptionCount.value)

            advanceTimeBy(2)
            assertEquals(0, repository.group.subscriptionCount.value)
        }
}
