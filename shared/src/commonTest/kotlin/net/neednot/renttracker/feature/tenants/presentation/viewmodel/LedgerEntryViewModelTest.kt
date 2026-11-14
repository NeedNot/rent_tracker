package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.FakeLedgerEntryRepository
import net.neednot.renttracker.feature.tenants.domain.FakeTenantRepository
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantRepositoryCall
import net.neednot.renttracker.feature.tenants.domain.chargeEntry
import net.neednot.renttracker.feature.tenants.domain.paymentEntry
import net.neednot.renttracker.feature.tenants.domain.rentEntry
import net.neednot.renttracker.feature.tenants.domain.synced
import net.neednot.renttracker.feature.tenants.domain.tenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.CANCEL_REASON_MAX_LENGTH
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerDialogUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerEntryUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.input_error_amount_format
import net.neednot.renttracker.resources.input_error_amount_positive
import net.neednot.renttracker.resources.input_error_amount_too_large
import net.neednot.renttracker.resources.ledger_bounced_message
import net.neednot.renttracker.resources.ledger_bounced_message_check
import net.neednot.renttracker.resources.ledger_cancel_reason_required
import net.neednot.renttracker.resources.ledger_message_custom_cancelled
import net.neednot.renttracker.resources.ledger_message_custom_restored
import net.neednot.renttracker.resources.ledger_message_fee_added
import net.neednot.renttracker.resources.ledger_message_late_fee_cancelled
import net.neednot.renttracker.resources.ledger_message_late_fee_restored
import net.neednot.renttracker.resources.ledger_message_payment_cancelled
import net.neednot.renttracker.resources.ledger_message_payment_restored
import net.neednot.renttracker.resources.tenant_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerEntryViewModelTest : MainDispatcherTest() {
    private val repository = FakeTenantRepository()
    private val ledgerEntries = FakeLedgerEntryRepository()
    private val messages = RecordingMessageDispatcher()
    private val viewModel by lazy { LedgerEntryViewModel("t1", repository, ledgerEntries, messages, TimeZone.UTC) }

    private val cancelled = EntryStatus.Cancelled("Entered twice", Person.You, Instant.parse("2026-03-05T12:00:00Z"))
    private val bounced = EntryStatus.Bounced(Person.You, Instant.parse("2026-02-19T12:00:00Z"))

    @Test
    fun nothingIsOpenUntilAnEntryIsTapped() =
        runTest {
            val state = showLedger()

            assertEquals(LedgerEntryUiState(sheet = null, dialog = null, isWorking = false), state())
        }

    @Test
    fun anActivePaymentOffersEditMarkBouncedAndCancel() =
        runTest {
            val state = showLedger()

            viewModel.onEntryClicked("payment:p1")
            runCurrent()

            assertEquals(
                listOf(LedgerSheetAction.Edit, LedgerSheetAction.MarkBounced, LedgerSheetAction.CancelPayment),
                state().sheet?.actions,
            )
        }

    @Test
    fun aBouncedPaymentOffersRestoreAndCancelButNoEdit() =
        runTest {
            val state = showLedger()

            viewModel.onEntryClicked("payment:p3")
            runCurrent()

            assertEquals(
                listOf(LedgerSheetAction.RestoreClearBounce, LedgerSheetAction.CancelPayment),
                state().sheet?.actions,
            )
        }

    @Test
    fun aCancelledPaymentOffersOnlyRestore() =
        runTest {
            val state = showLedger()

            viewModel.onEntryClicked("payment:p2")
            runCurrent()

            assertEquals(listOf(LedgerSheetAction.Restore), state().sheet?.actions)
        }

    @Test
    fun aChargeOffersEditAndCancelCharge() =
        runTest {
            val state = showLedger()

            viewModel.onEntryClicked("debt:d1")
            runCurrent()

            assertEquals(listOf(LedgerSheetAction.Edit, LedgerSheetAction.CancelCharge), state().sheet?.actions)
        }

    @Test
    fun anEntryTheCallerCannotChangeShowsNoActions() =
        runTest {
            val state = showLedger(paymentEntry(key = "payment:p1", canChange = false))

            viewModel.onEntryClicked("payment:p1")
            runCurrent()

            assertEquals(emptyList(), state().sheet?.actions)
        }

    @Test
    fun aRentRowOpensNothing() =
        runTest {
            val state = showLedger()

            viewModel.onEntryClicked("rent:2026-03")
            runCurrent()

            assertNull(state().sheet)
        }

    @Test
    fun dismissingTheSheetClosesItAndItsDialog() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelPayment)
            runCurrent()

            viewModel.onSheetDismissed()
            runCurrent()

            assertEquals(LedgerEntryUiState(sheet = null, dialog = null, isWorking = false), state())
        }

    @Test
    fun cancellingNeedsAReasonAndAnEmptyOneIsBlockedBeforeAnyCall() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelPayment)

            viewModel.onDialogConfirmed()
            runCurrent()

            val dialog = assertIs<LedgerDialogUiState.Cancel>(state().dialog)
            assertEquals(UiText.Resource(Res.string.ledger_cancel_reason_required), dialog.error)
            assertTrue(ledgerEntries.calls.isEmpty())
        }

    @Test
    fun clearingTheReasonFieldShowsTheErrorAsYouType() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelPayment)
            viewModel.onDialogTextChanged("Dup")
            runCurrent()
            assertNull(assertIs<LedgerDialogUiState.Cancel>(state().dialog).error)

            viewModel.onDialogTextChanged("  ")
            runCurrent()

            val dialog = assertIs<LedgerDialogUiState.Cancel>(state().dialog)
            assertEquals(UiText.Resource(Res.string.ledger_cancel_reason_required), dialog.error)
        }

    @Test
    fun theReasonIsCappedAtOneHundredCharacters() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelPayment)

            viewModel.onDialogTextChanged("x".repeat(CANCEL_REASON_MAX_LENGTH + 20))
            runCurrent()

            assertEquals(CANCEL_REASON_MAX_LENGTH, assertIs<LedgerDialogUiState.Cancel>(state().dialog).reason.length)
        }

    @Test
    fun cancellingAPaymentSendsTheReasonAndDisplayedVersionThenClosesAndAnnounces() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelPayment)
            viewModel.onDialogTextChanged("  Entered twice ")

            viewModel.onDialogConfirmed()
            runCurrent()

            assertEquals(
                listOf<TenantRepositoryCall>(TenantRepositoryCall.CancelPayment("p1", 4, "Entered twice")),
                ledgerEntries.calls,
            )
            assertEquals(LedgerEntryUiState(sheet = null, dialog = null, isWorking = false), state())
            assertEquals(
                listOf(UiMessage(UiText.Resource(Res.string.ledger_message_payment_cancelled))),
                messages.shown,
            )
        }

    @Test
    fun cancellingALateFeeNamesItsMonth() =
        runTest {
            showLedger()
            viewModel.onEntryClicked("debt:d2")
            viewModel.onCancelClicked(LedgerSheetAction.CancelCharge)
            viewModel.onDialogTextChanged("Mistake")

            viewModel.onDialogConfirmed()
            runCurrent()

            assertEquals(
                listOf<TenantRepositoryCall>(TenantRepositoryCall.CancelDebt("d2", 5, "Mistake")),
                ledgerEntries.calls,
            )
            assertEquals(
                listOf(UiMessage(UiText.Resource(Res.string.ledger_message_late_fee_cancelled, listOf("February")))),
                messages.shown,
            )
        }

    @Test
    fun cancellingACustomDebtNamesItsNote() =
        runTest {
            showLedger()
            viewModel.onEntryClicked("debt:d1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelCharge)
            viewModel.onDialogTextChanged("Mistake")

            viewModel.onDialogConfirmed()
            runCurrent()

            assertEquals(
                listOf(
                    UiMessage(UiText.Resource(Res.string.ledger_message_custom_cancelled, listOf("Water bill, Feb"))),
                ),
                messages.shown,
            )
        }

    @Test
    fun aServerRefusalStaysInTheCancelDialogWithItsMessage() =
        runTest {
            ledgerEntries.changeResult =
                Err(TenantError.EditConflict("Sam changed this payment while you were looking."))
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelPayment)
            viewModel.onDialogTextChanged("Duplicate")

            viewModel.onDialogConfirmed()
            runCurrent()

            val dialog = assertIs<LedgerDialogUiState.Cancel>(state().dialog)
            assertEquals(UiText.Plain("Sam changed this payment while you were looking."), dialog.error)
            assertEquals(false, dialog.isWorking)
            assertTrue(messages.shown.isEmpty())
        }

    @Test
    fun aPermissionRefusalShowsTheServersMessage() =
        runTest {
            ledgerEntries.changeResult = Err(TenantError.PermissionDenied("You can only change entries you added."))
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelPayment)
            viewModel.onDialogTextChanged("Duplicate")

            viewModel.onDialogConfirmed()
            runCurrent()

            assertEquals(
                UiText.Plain("You can only change entries you added."),
                assertIs<LedgerDialogUiState.Cancel>(state().dialog).error,
            )
        }

    @Test
    fun cancellingWithoutAConnectionSaysSo() =
        runTest {
            ledgerEntries.changeResult = Err(TenantError.Offline)
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelPayment)
            viewModel.onDialogTextChanged("Duplicate")

            viewModel.onDialogConfirmed()
            runCurrent()

            assertEquals(
                UiText.Resource(Res.string.tenant_error_offline),
                assertIs<LedgerDialogUiState.Cancel>(state().dialog).error,
            )
        }

    @Test
    fun keepingTheEntryClosesTheDialogAndCancelsNothing() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onCancelClicked(LedgerSheetAction.CancelPayment)

            viewModel.onDialogDismissed()
            runCurrent()

            assertNull(state().dialog)
            assertTrue(ledgerEntries.calls.isEmpty())
        }

    @Test
    fun markingBouncedAsksFirstWithTheDateKindAndAmount() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")

            viewModel.onMarkBouncedClicked()
            runCurrent()

            val dialog = assertIs<LedgerDialogUiState.ConfirmBounced>(state().dialog)
            assertEquals(
                UiText.Resource(Res.string.ledger_bounced_message, listOf("Mar 2", "$1,500.00")),
                dialog.message,
            )
            assertTrue(ledgerEntries.calls.isEmpty())
        }

    @Test
    fun theBouncedQuestionSaysCheckForACheck() =
        runTest {
            val state = showLedger(paymentEntry(key = "payment:p1", method = PaymentMethod.Check))
            viewModel.onEntryClicked("payment:p1")

            viewModel.onMarkBouncedClicked()
            runCurrent()

            val dialog = assertIs<LedgerDialogUiState.ConfirmBounced>(state().dialog)
            assertEquals(
                UiText.Resource(Res.string.ledger_bounced_message_check, listOf("Mar 2", "$1,500.00")),
                dialog.message,
            )
        }

    @Test
    fun confirmingBouncedSendsTheDisplayedVersionAndThenOffersTheFee() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onMarkBouncedClicked()

            viewModel.onDialogConfirmed()
            runCurrent()

            assertEquals(
                listOf<TenantRepositoryCall>(TenantRepositoryCall.MarkPaymentBounced("p1", 4)),
                ledgerEntries.calls,
            )
            assertEquals(LedgerDialogUiState.BouncedFee(), state().dialog)
        }

    @Test
    fun aRefusedBouncedMarkStaysInTheDialogWithTheServersMessage() =
        runTest {
            ledgerEntries.changeResult = Err(TenantError.EditConflict("Sam already changed this payment."))
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onMarkBouncedClicked()

            viewModel.onDialogConfirmed()
            runCurrent()

            val dialog = assertIs<LedgerDialogUiState.ConfirmBounced>(state().dialog)
            assertEquals(UiText.Plain("Sam already changed this payment."), dialog.error)
        }

    @Test
    fun addingTheBouncedCheckFeeUsesTodayAndAnnounces() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onMarkBouncedClicked()
            viewModel.onDialogConfirmed()
            runCurrent()
            viewModel.onDialogTextChanged("35")

            viewModel.onDialogConfirmed()
            runCurrent()

            assertEquals(
                listOf<TenantRepositoryCall>(
                    TenantRepositoryCall.MarkPaymentBounced("p1", 4),
                    TenantRepositoryCall.AddBouncedCheckFee("t1", 3_500, LocalDate(2026, 3, 10)),
                ),
                ledgerEntries.calls,
            )
            assertNull(state().dialog)
            assertEquals(listOf(UiMessage(UiText.Resource(Res.string.ledger_message_fee_added))), messages.shown)
        }

    @Test
    fun skippingTheFeeAddsNothing() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onMarkBouncedClicked()
            viewModel.onDialogConfirmed()
            runCurrent()

            viewModel.onDialogDismissed()
            runCurrent()

            assertNull(state().dialog)
            assertEquals(
                listOf<TenantRepositoryCall>(TenantRepositoryCall.MarkPaymentBounced("p1", 4)),
                ledgerEntries.calls,
            )
        }

    @Test
    fun aFeeAmountThatIsNotAnAmountIsBlockedBeforeAnyCall() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onMarkBouncedClicked()
            viewModel.onDialogConfirmed()
            runCurrent()
            val callsBefore = ledgerEntries.calls.size

            viewModel.onDialogTextChanged("abc")
            viewModel.onDialogConfirmed()
            runCurrent()

            val dialog = assertIs<LedgerDialogUiState.BouncedFee>(state().dialog)
            assertEquals(UiText.Resource(Res.string.input_error_amount_format), dialog.error)
            assertEquals(callsBefore, ledgerEntries.calls.size)
        }

    @Test
    fun aFeeAmountErrorAppearsAsItIsTyped() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onMarkBouncedClicked()
            viewModel.onDialogConfirmed()
            runCurrent()

            viewModel.onDialogTextChanged("0.00")
            runCurrent()
            assertEquals(
                UiText.Resource(Res.string.input_error_amount_positive),
                assertIs<LedgerDialogUiState.BouncedFee>(state().dialog).error,
            )

            viewModel.onDialogTextChanged("1,000,000.01")
            runCurrent()
            assertEquals(
                UiText.Resource(Res.string.input_error_amount_too_large),
                assertIs<LedgerDialogUiState.BouncedFee>(state().dialog).error,
            )
        }

    @Test
    fun anEmptyFeeAmountIsBlockedToo() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onMarkBouncedClicked()
            viewModel.onDialogConfirmed()
            runCurrent()

            viewModel.onDialogConfirmed()
            runCurrent()

            assertEquals(
                UiText.Resource(Res.string.input_error_amount_format),
                assertIs<LedgerDialogUiState.BouncedFee>(state().dialog).error,
            )
        }

    @Test
    fun aRefusedFeeStaysInTheDialogWithTheServersMessage() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p1")
            viewModel.onMarkBouncedClicked()
            viewModel.onDialogConfirmed()
            runCurrent()
            ledgerEntries.changeResult = Err(TenantError.TenantArchived("This tenant is archived."))
            viewModel.onDialogTextChanged("35")

            viewModel.onDialogConfirmed()
            runCurrent()

            val dialog = assertIs<LedgerDialogUiState.BouncedFee>(state().dialog)
            assertEquals(UiText.Plain("This tenant is archived."), dialog.error)
            assertTrue(messages.shown.isEmpty())
        }

    @Test
    fun restoringAPaymentFromTheSheetSendsTheVersionClosesAndAnnounces() =
        runTest {
            val state = showLedger()
            viewModel.onEntryClicked("payment:p3")

            viewModel.onRestoreEntryClicked("payment:p3")
            runCurrent()

            assertEquals(
                listOf<TenantRepositoryCall>(TenantRepositoryCall.RestorePayment("p3", 6)),
                ledgerEntries.calls,
            )
            assertNull(state().sheet)
            assertEquals(listOf(UiMessage(UiText.Resource(Res.string.ledger_message_payment_restored))), messages.shown)
        }

    @Test
    fun restoringFromTheRowRestoresWithoutOpeningTheSheet() =
        runTest {
            val state = showLedger()

            viewModel.onRestoreEntryClicked("payment:p2")
            runCurrent()

            assertEquals(
                listOf<TenantRepositoryCall>(TenantRepositoryCall.RestorePayment("p2", 5)),
                ledgerEntries.calls,
            )
            assertNull(state().sheet)
            assertEquals(listOf(UiMessage(UiText.Resource(Res.string.ledger_message_payment_restored))), messages.shown)
        }

    @Test
    fun restoringADebtAnnouncesItByName() =
        runTest {
            showLedger()

            viewModel.onRestoreEntryClicked("debt:d3")
            runCurrent()
            viewModel.onRestoreEntryClicked("debt:d4")
            runCurrent()

            assertEquals(
                listOf<TenantRepositoryCall>(
                    TenantRepositoryCall.RestoreDebt("d3", 7),
                    TenantRepositoryCall.RestoreDebt("d4", 8),
                ),
                ledgerEntries.calls,
            )
            assertEquals(
                listOf(
                    UiMessage(UiText.Resource(Res.string.ledger_message_late_fee_restored, listOf("February"))),
                    UiMessage(UiText.Resource(Res.string.ledger_message_custom_restored, listOf("Water bill, Feb"))),
                ),
                messages.shown,
            )
        }

    @Test
    fun aRefusedRestoreIsAnErrorMessageWithTheServersText() =
        runTest {
            ledgerEntries.changeResult = Err(TenantError.PermissionDenied("You can only change entries you added."))
            val state = showLedger()
            viewModel.onEntryClicked("payment:p3")

            viewModel.onRestoreEntryClicked("payment:p3")
            runCurrent()

            assertEquals(
                listOf(UiMessage(UiText.Plain("You can only change entries you added."), UiMessageVariant.Error)),
                messages.shown,
            )
            assertEquals(false, state().isWorking)
            assertEquals(true, state().sheet != null)
        }

    private fun TestScope.showLedger(vararg replacements: LedgerEntry): () -> LedgerEntryUiState {
        val ledger =
            listOf(
                paymentEntry(key = "payment:p1", paymentId = "p1", version = 4),
                paymentEntry(key = "payment:p2", paymentId = "p2", version = 5, status = cancelled, balance = null),
                paymentEntry(key = "payment:p3", paymentId = "p3", version = 6, status = bounced, balance = null),
                chargeEntry(key = "debt:d1", debtId = "d1", version = 2, kind = ChargeKind.Custom("Water bill, Feb")),
                chargeEntry(key = "debt:d2", debtId = "d2", version = 5, kind = ChargeKind.LateFee(YearMonth(2026, 2))),
                chargeEntry(
                    key = "debt:d3",
                    debtId = "d3",
                    version = 7,
                    kind = ChargeKind.LateFee(YearMonth(2026, 2)),
                    status = cancelled,
                    balance = null,
                ),
                chargeEntry(
                    key = "debt:d4",
                    debtId = "d4",
                    version = 8,
                    kind = ChargeKind.Custom("Water bill, Feb"),
                    status = cancelled,
                    balance = null,
                ),
                rentEntry(month = "2026-03"),
            )
        val replacedKeys = replacements.map { it.key }.toSet()
        repository.tenant.value =
            synced(tenantOverview(ledger = replacements.toList() + ledger.filter { it.key !in replacedKeys }))
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return { viewModel.uiState.value }
    }
}
