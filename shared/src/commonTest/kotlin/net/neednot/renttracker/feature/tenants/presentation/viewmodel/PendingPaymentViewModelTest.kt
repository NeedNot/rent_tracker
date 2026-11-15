package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DuplicateMatchKind
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.FakePendingPaymentRepository
import net.neednot.renttracker.feature.tenants.domain.PendingBalanceChange
import net.neednot.renttracker.feature.tenants.domain.PendingNewEntry
import net.neednot.renttracker.feature.tenants.domain.PendingPaymentReview
import net.neednot.renttracker.feature.tenants.domain.PendingRejection
import net.neednot.renttracker.feature.tenants.domain.PendingState
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.duplicateMatch
import net.neednot.renttracker.feature.tenants.domain.pendingPayment
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerFieldUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChangeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingMatchUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingPaymentUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetBody
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_credit
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.ledger_field_date_received
import net.neednot.renttracker.resources.ledger_field_method
import net.neednot.renttracker.resources.ledger_method_zelle
import net.neednot.renttracker.resources.ledger_payment_title
import net.neednot.renttracker.resources.pending_changed_intro
import net.neednot.renttracker.resources.pending_changed_title
import net.neednot.renttracker.resources.pending_duplicate_intro
import net.neednot.renttracker.resources.pending_duplicate_title
import net.neednot.renttracker.resources.pending_part_late_fee
import net.neednot.renttracker.resources.pending_part_you
import net.neednot.renttracker.resources.pending_rejected_archived
import net.neednot.renttracker.resources.pending_rejected_moved
import net.neednot.renttracker.resources.pending_rejected_no_permission
import net.neednot.renttracker.resources.pending_rejected_not_saved
import net.neednot.renttracker.resources.pending_removed
import net.neednot.renttracker.resources.pending_sheet_not_sent
import net.neednot.renttracker.resources.pending_sheet_title
import net.neednot.renttracker.resources.tenant_error_not_found
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PendingPaymentViewModelTest : MainDispatcherTest() {
    private val repository = FakePendingPaymentRepository()
    private val messages = RecordingMessageDispatcher()

    private val waiting = PendingPaymentReview.Waiting(pendingPayment())

    @Test
    fun nothingIsOpenUntilAPendingPaymentIsTapped() =
        runTest {
            repository.reviews.value = mapOf("c1" to waiting)

            assertEquals(PendingPaymentUiState(), stateOf(viewModel()))
        }

    @Test
    fun aNotificationTapOpensItsPaymentOnArrival() =
        runTest {
            repository.reviews.value = mapOf("c1" to waiting)

            assertEquals("c1", stateOf(viewModel(openedClientId = "c1")).sheet?.clientId)
        }

    @Test
    fun aWaitingPaymentShowsItsDetailsWithEditAndCancel() =
        runTest {
            val sheet = openSheet(waiting)

            assertEquals(
                PendingSheetUiState(
                    clientId = "c1",
                    title = UiText.Resource(Res.string.pending_sheet_title, listOf("Apt 3B")),
                    chip = PendingChip.Pending,
                    amount = "$550.00",
                    body =
                        PendingSheetBody.Waiting(
                            message = UiText.Resource(Res.string.pending_sheet_not_sent),
                            fields =
                                listOf(
                                    LedgerFieldUiState(
                                        UiText.Resource(Res.string.ledger_field_date_received),
                                        UiText.Plain("Mar 10, 2026"),
                                    ),
                                    LedgerFieldUiState(
                                        UiText.Resource(Res.string.ledger_field_method),
                                        UiText.Resource(Res.string.ledger_method_zelle),
                                    ),
                                ),
                        ),
                    actions = listOf(PendingSheetAction.Edit, PendingSheetAction.Cancel),
                ),
                sheet,
            )
        }

    @Test
    fun aPaymentWithoutAMethodHasNoMethodRow() =
        runTest {
            val sheet = openSheet(PendingPaymentReview.Waiting(pendingPayment(method = null)))

            assertEquals(1, assertIs<PendingSheetBody.Waiting>(sheet?.body).fields.size)
        }

    @Test
    fun aPossibleDuplicateListsTheMatchesWithSaveAnywayAndCancelPayment() =
        runTest {
            val review =
                PendingPaymentReview.PossibleDuplicate(
                    pendingPayment(state = PendingState.PossibleDuplicate),
                    listOf(duplicateMatch(), duplicateMatch(kind = DuplicateMatchKind.LateFee, method = null)),
                )

            val sheet = openSheet(review)

            assertEquals(PendingChip.NeedsConfirmation, sheet?.chip)
            assertEquals(
                PendingSheetBody.PossibleDuplicate(
                    title = UiText.Resource(Res.string.pending_duplicate_title),
                    intro = UiText.Resource(Res.string.pending_duplicate_intro, listOf("Apt 3B")),
                    matches =
                        listOf(
                            PendingMatchUiState(
                                parts =
                                    listOf(
                                        UiText.Plain("Mar 9"),
                                        UiText.Resource(Res.string.ledger_method_zelle),
                                        UiText.Plain("Sam Rivera"),
                                    ),
                                amount = "$550.00",
                            ),
                            PendingMatchUiState(
                                parts =
                                    listOf(
                                        UiText.Resource(Res.string.pending_part_late_fee),
                                        UiText.Plain("Mar 9"),
                                        UiText.Plain("Sam Rivera"),
                                    ),
                                amount = "$550.00",
                            ),
                        ),
                ),
                sheet?.body,
            )
            assertEquals(listOf(PendingSheetAction.SaveAnyway, PendingSheetAction.CancelPayment), sheet?.actions)
        }

    @Test
    fun aChangedTenantShowsTheBalanceChangeAndNewEntries() =
        runTest {
            val review =
                PendingPaymentReview.TenantChanged(
                    payment = pendingPayment(state = PendingState.TenantChanged),
                    balanceChange = PendingBalanceChange(wasCents = 55_000, nowCents = -2_500),
                    newEntries =
                        listOf(
                            PendingNewEntry.Payment(LocalDate(2026, 3, 10), 55_000, Person.Named("Sam Rivera")),
                            PendingNewEntry.Charge(
                                LocalDate(2026, 3, 11),
                                5_000,
                                Person.You,
                                ChargeKind.LateFee(YearMonth(2026, 3)),
                            ),
                        ),
                )

            val sheet = openSheet(review)

            assertEquals(
                PendingSheetBody.TenantChanged(
                    title = UiText.Resource(Res.string.pending_changed_title),
                    intro = UiText.Resource(Res.string.pending_changed_intro),
                    changes =
                        listOf(
                            PendingChangeUiState.Balance(
                                was = UiText.Resource(Res.string.balance_owes, listOf("$550.00")),
                                now = UiText.Resource(Res.string.balance_credit, listOf("$25.00")),
                            ),
                            PendingChangeUiState.NewEntry(
                                listOf(
                                    UiText.Resource(Res.string.ledger_payment_title),
                                    UiText.Plain("Mar 10"),
                                    UiText.Plain("Sam Rivera"),
                                    UiText.Plain("$550.00"),
                                ),
                            ),
                            PendingChangeUiState.NewEntry(
                                listOf(
                                    UiText.Resource(Res.string.pending_part_late_fee),
                                    UiText.Plain("Mar 11"),
                                    UiText.Resource(Res.string.pending_part_you),
                                    UiText.Plain("$50.00"),
                                ),
                            ),
                        ),
                ),
                sheet?.body,
            )
            assertEquals(
                listOf(PendingSheetAction.SendAnyway, PendingSheetAction.Edit, PendingSheetAction.CancelPayment),
                sheet?.actions,
            )
        }

    @Test
    fun aZeroBalanceReadsAsZeroDollars() =
        runTest {
            val review =
                PendingPaymentReview.TenantChanged(
                    payment = pendingPayment(state = PendingState.TenantChanged),
                    balanceChange = PendingBalanceChange(wasCents = 55_000, nowCents = 0),
                    newEntries = emptyList(),
                )

            val changes = assertIs<PendingSheetBody.TenantChanged>(openSheet(review)?.body).changes

            assertEquals(
                listOf<PendingChangeUiState>(
                    PendingChangeUiState.Balance(
                        was = UiText.Resource(Res.string.balance_owes, listOf("$550.00")),
                        now = UiText.Plain("$0.00"),
                    ),
                ),
                changes,
            )
        }

    @Test
    fun aRejectedPaymentShowsTheReasonAndOnlyDismiss() =
        runTest {
            val cases =
                listOf(
                    PendingRejection.NoPermission to UiText.Resource(Res.string.pending_rejected_no_permission),
                    PendingRejection.TenantArchived to UiText.Resource(Res.string.pending_rejected_archived),
                    PendingRejection.TenantMoved to UiText.Resource(Res.string.pending_rejected_moved),
                    PendingRejection.Other("Group was deleted") to UiText.Plain("Group was deleted"),
                )

            cases.forEach { (reason, text) ->
                val review =
                    PendingPaymentReview.Rejected(
                        pendingPayment(state = PendingState.Rejected(reason)),
                        reason,
                    )

                val sheet = openSheet(review)

                assertEquals(PendingChip.Rejected, sheet?.chip)
                assertEquals(
                    PendingSheetBody.Rejected(text, UiText.Resource(Res.string.pending_rejected_not_saved)),
                    sheet?.body,
                )
                assertEquals(listOf(PendingSheetAction.Dismiss), sheet?.actions)
            }
        }

    @Test
    fun editClosesTheSheet() =
        runTest {
            val viewModel = viewModel()
            open(viewModel, waiting)

            viewModel.onActionClicked(PendingSheetAction.Edit)

            assertNull(stateOf(viewModel).sheet)
        }

    @Test
    fun cancelAsksFirstAndKeepLeavesThePayment() =
        runTest {
            val viewModel = viewModel()
            open(viewModel, waiting)

            viewModel.onActionClicked(PendingSheetAction.Cancel)
            assertTrue(stateOf(viewModel).isCancelDialogOpen)

            viewModel.onCancelDialogDismissed()
            val state = stateOf(viewModel)

            assertFalse(state.isCancelDialogOpen)
            assertEquals("c1", state.sheet?.clientId)
            assertEquals(emptyList(), repository.discarded)
        }

    @Test
    fun confirmingTheCancelRemovesThePaymentAndSaysSo() =
        runTest {
            val viewModel = viewModel()
            open(viewModel, waiting)

            viewModel.onActionClicked(PendingSheetAction.Cancel)
            viewModel.onCancelConfirmed()
            val state = stateOf(viewModel)

            assertEquals(listOf("c1"), repository.discarded)
            assertNull(state.sheet)
            assertEquals(listOf(UiMessage(UiText.Resource(Res.string.pending_removed))), messages.shown)
        }

    @Test
    fun saveAnywayAndSendAnywayConfirmThePayment() =
        runTest {
            listOf(PendingSheetAction.SaveAnyway, PendingSheetAction.SendAnyway).forEach { action ->
                val viewModel = viewModel()
                open(viewModel, waiting)

                viewModel.onActionClicked(action)

                assertNull(stateOf(viewModel).sheet)
            }

            assertEquals(listOf("c1", "c1"), repository.confirmed)
        }

    @Test
    fun aFailedConfirmKeepsTheSheetAndShowsTheError() =
        runTest {
            repository.confirmResult = Err(TenantError.NotFound)
            val viewModel = viewModel()
            open(viewModel, waiting)

            viewModel.onActionClicked(PendingSheetAction.SaveAnyway)

            assertEquals("c1", stateOf(viewModel).sheet?.clientId)
            assertEquals(
                listOf(UiMessage(UiText.Resource(Res.string.tenant_error_not_found), UiMessageVariant.Error)),
                messages.shown,
            )
        }

    @Test
    fun cancelPaymentRemovesWithoutAskingAgain() =
        runTest {
            val viewModel = viewModel()
            open(viewModel, waiting)

            viewModel.onActionClicked(PendingSheetAction.CancelPayment)
            runCurrent()

            assertEquals(listOf("c1"), repository.discarded)
            assertEquals(listOf(UiMessage(UiText.Resource(Res.string.pending_removed))), messages.shown)
        }

    @Test
    fun dismissRemovesARejectedPaymentQuietly() =
        runTest {
            val viewModel = viewModel()
            open(viewModel, waiting)

            viewModel.onActionClicked(PendingSheetAction.Dismiss)
            val state = stateOf(viewModel)

            assertEquals(listOf("c1"), repository.discarded)
            assertNull(state.sheet)
            assertEquals(emptyList(), messages.shown)
        }

    @Test
    fun theSheetClosesWhenThePaymentSyncs() =
        runTest {
            val viewModel = viewModel()
            open(viewModel, waiting)

            repository.reviews.value = emptyMap()

            assertNull(stateOf(viewModel).sheet)
        }

    private fun viewModel(openedClientId: String? = null) =
        PendingPaymentViewModel(openedClientId, repository, messages)

    private suspend fun TestScope.openSheet(review: PendingPaymentReview): PendingSheetUiState? {
        val viewModel = viewModel()
        open(viewModel, review)
        return stateOf(viewModel).sheet
    }

    private fun TestScope.open(
        viewModel: PendingPaymentViewModel,
        review: PendingPaymentReview,
    ) {
        repository.reviews.value = mapOf(review.payment.clientId to review)
        viewModel.onPendingPaymentClicked(review.payment.clientId)
        runCurrent()
    }

    private suspend fun TestScope.stateOf(viewModel: PendingPaymentViewModel): PendingPaymentUiState {
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel.uiState.first()
    }
}
