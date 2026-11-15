package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.FakeMoveTenantRepository
import net.neednot.renttracker.feature.tenants.domain.FakeTenantRepository
import net.neednot.renttracker.feature.tenants.domain.MoveTarget
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.synced
import net.neednot.renttracker.feature.tenants.domain.tenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveConfirmationUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveTargetUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveTenantUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_tenant_count
import net.neednot.renttracker.resources.move_confirm_gain_access
import net.neednot.renttracker.resources.move_confirm_history
import net.neednot.renttracker.resources.move_confirm_lose_access
import net.neednot.renttracker.resources.move_confirm_settings_by_date
import net.neednot.renttracker.resources.move_confirm_settings_rent_first
import net.neednot.renttracker.resources.move_confirm_title
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MoveTenantViewModelTest : MainDispatcherTest() {
    private val repository = FakeTenantRepository()
    private val moveRepository = FakeMoveTenantRepository()
    private val messages = RecordingMessageDispatcher()
    private val viewModel by lazy { MoveTenantViewModel("t1", repository, moveRepository, messages) }

    private val mapleCourt =
        MoveTarget(
            groupId = "g2",
            name = "Maple Court",
            tenantCount = 4,
            paymentOrder = PaymentOrder.ByDate,
            graceDays = 3,
        )
    private val elmStreet =
        MoveTarget(
            groupId = "g3",
            name = "Elm Street Fourplex",
            tenantCount = 3,
            paymentOrder = PaymentOrder.RentFirst,
            graceDays = 5,
        )

    private fun targetUi(
        target: MoveTarget,
        isSelected: Boolean,
    ) = MoveTargetUiState(
        groupId = target.groupId,
        name = target.name,
        tenantCount = UiText.Plural(Res.plurals.home_tenant_count, target.tenantCount, listOf(target.tenantCount)),
        isSelected = isSelected,
    )

    @Test
    fun showsLoadingUntilBothTheTenantAndTheTargetsArrive() =
        runTest {
            viewModel.uiState.test {
                assertEquals(MoveTenantUiState.Loading, awaitItem())

                repository.tenant.value = synced(tenantOverview())
                runCurrent()
                expectNoEvents()
            }
        }

    @Test
    fun listsTheTargetsWithTheFirstOneSelected() =
        runTest {
            showing(listOf(mapleCourt, elmStreet))

            viewModel.uiState.test {
                skipItems(1)

                val content = awaitItem() as MoveTenantUiState.Content
                assertEquals("Apt 3B", content.tenantName)
                assertEquals(listOf(targetUi(mapleCourt, true), targetUi(elmStreet, false)), content.targets)
                assertEquals(null, content.confirmation)
            }
        }

    @Test
    fun tappingATargetSelectsIt() =
        runTest {
            showing(listOf(mapleCourt, elmStreet))

            viewModel.uiState.test {
                skipItems(2)

                viewModel.onTargetClicked("g3")

                val content = awaitItem() as MoveTenantUiState.Content
                assertEquals(listOf(false, true), content.targets.map { it.isSelected })
            }
        }

    @Test
    fun withNoTargetsTheEmptyStateNamesTheTenant() =
        runTest {
            showing(emptyList())

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(MoveTenantUiState.Empty("Apt 3B"), awaitItem())
            }
        }

    @Test
    fun aFailedTargetQueryShowsTheError() =
        runTest {
            repository.tenant.value = synced(tenantOverview())
            moveRepository.targets.value = Err(TenantError.Offline)

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(MoveTenantUiState.Error(TenantError.Offline.text()), awaitItem())
            }
        }

    @Test
    fun aTenantThatFailsToLoadShowsTheError() =
        runTest {
            repository.tenant.value = Synced(null, SyncStatus.Failed(TenantError.NotFound))
            moveRepository.targets.value = Ok(listOf(mapleCourt))

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(MoveTenantUiState.Error(TenantError.NotFound.text()), awaitItem())
            }
        }

    @Test
    fun nextAsksToConfirmWithBothGroupNamesAndTheTargetsSettings() =
        runTest {
            showing(listOf(mapleCourt, elmStreet))

            viewModel.uiState.test {
                skipItems(2)

                viewModel.onNextClicked()

                val confirmation = (awaitItem() as MoveTenantUiState.Content).confirmation
                assertEquals(
                    MoveConfirmationUiState(
                        title = UiText.Resource(Res.string.move_confirm_title, listOf("Apt 3B", "Maple Court")),
                        consequences =
                            listOf(
                                UiText.Resource(Res.string.move_confirm_lose_access, listOf("1st Street")),
                                UiText.Resource(Res.string.move_confirm_gain_access, listOf("Maple Court")),
                                UiText.Resource(Res.string.move_confirm_settings_by_date, listOf("Maple Court", 3)),
                                UiText.Resource(Res.string.move_confirm_history),
                            ),
                    ),
                    confirmation,
                )
            }
        }

    @Test
    fun aRentFirstTargetSaysSo() =
        runTest {
            showing(listOf(elmStreet))

            viewModel.uiState.test {
                skipItems(2)

                viewModel.onNextClicked()

                val confirmation = (awaitItem() as MoveTenantUiState.Content).confirmation
                assertEquals(
                    UiText.Resource(Res.string.move_confirm_settings_rent_first, listOf("Elm Street Fourplex", 5)),
                    confirmation?.consequences?.get(2),
                )
            }
        }

    @Test
    fun cancellingTheConfirmationMovesNothing() =
        runTest {
            showing(listOf(mapleCourt))

            viewModel.uiState.test {
                skipItems(2)
                viewModel.onNextClicked()
                skipItems(1)

                viewModel.onConfirmationDismissed()

                assertEquals(null, (awaitItem() as MoveTenantUiState.Content).confirmation)
            }
            assertEquals(emptyList<Triple<String, String, Long>>(), moveRepository.moves)
        }

    @Test
    fun confirmingMovesTheTenantWithTheVersionOnScreenAndClosesTheScreen() =
        runTest {
            showing(listOf(mapleCourt, elmStreet), version = 7)

            viewModel.uiState.test {
                skipItems(2)
                viewModel.onTargetClicked("g3")
                viewModel.onNextClicked()
                skipItems(1)

                viewModel.onMoveConfirmed()

                assertEquals<MoveTenantEvent>(MoveTenantEvent.TenantMoved, viewModel.events.first())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf(Triple("t1", "g3", 7L)), moveRepository.moves)
        }

    @Test
    fun aMoveConflictIsShownInTheSnackbarAndClosesTheConfirmation() =
        runTest {
            showing(listOf(mapleCourt))
            moveRepository.moveResult = Err(TenantError.EditConflict("Sam Rivera changed this tenant."))

            viewModel.uiState.test {
                skipItems(2)
                viewModel.onNextClicked()
                skipItems(1)

                viewModel.onMoveConfirmed()

                awaitSettled()
            }
            assertEquals(
                listOf(UiMessage(UiText.Plain("Sam Rivera changed this tenant."), UiMessageVariant.Error)),
                messages.shown,
            )
        }

    @Test
    fun aDeniedMoveIsShownInTheSnackbar() =
        runTest {
            showing(listOf(mapleCourt))
            moveRepository.moveResult = Err(TenantError.PermissionDenied("You can't move tenants here."))

            viewModel.uiState.test {
                skipItems(2)
                viewModel.onNextClicked()
                skipItems(1)

                viewModel.onMoveConfirmed()
                awaitSettled()
            }

            assertEquals(
                listOf(UiMessage(UiText.Plain("You can't move tenants here."), UiMessageVariant.Error)),
                messages.shown,
            )
        }

    private suspend fun ReceiveTurbine<MoveTenantUiState>.awaitSettled(): MoveTenantUiState.Content {
        while (true) {
            val item = awaitItem()
            if (item is MoveTenantUiState.Content && !item.isMoving && item.confirmation == null) return item
        }
    }

    private fun showing(
        targets: List<MoveTarget>,
        version: Long = 3,
    ) {
        repository.tenant.value = synced(tenantOverview(version = version))
        moveRepository.targets.value = Ok(targets)
    }
}
