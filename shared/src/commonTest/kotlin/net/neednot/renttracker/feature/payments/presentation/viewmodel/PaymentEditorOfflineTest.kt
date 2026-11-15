package net.neednot.renttracker.feature.payments.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.domain.FakePaymentRepository
import net.neednot.renttracker.feature.payments.domain.FakePaymentSyncAlerts
import net.neednot.renttracker.feature.payments.domain.LoggedPayment
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PaymentRepositoryCall
import net.neednot.renttracker.feature.payments.domain.PendingPayment
import net.neednot.renttracker.feature.payments.domain.paymentOverview
import net.neednot.renttracker.feature.payments.domain.synced
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.entry_error_not_found
import net.neednot.renttracker.resources.offline_banner
import net.neednot.renttracker.resources.payment_saved_on_phone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentEditorOfflineTest : MainDispatcherTest() {
    private val repository = FakePaymentRepository()
    private val syncAlerts = FakePaymentSyncAlerts()
    private val messages = RecordingMessageDispatcher()

    private val savedOnPhoneMessage = UiMessage(UiText.Resource(Res.string.payment_saved_on_phone))

    private val pendingDraft =
        PaymentDraft(
            amountCents = 42_000,
            dateReceived = LocalDate(2026, 3, 5),
            method = PaymentMethod.Venmo,
            note = "Half now",
        )

    private fun TestScope.openEditor(pendingClientId: String? = null): PaymentEditorViewModel {
        val editor =
            PaymentEditorViewModel(
                tenantId = "t1",
                paymentId = null,
                pendingClientId = pendingClientId,
                paymentRepository = repository,
                syncAlerts = syncAlerts,
                messageDispatcher = messages,
            )
        runCurrent()
        return editor
    }

    private fun goOffline() {
        repository.freshness.value = Freshness.Offline(updatedAgo = null)
    }

    private fun TestScope.saveOnPhone(editor: PaymentEditorViewModel) {
        repository.logResult = Ok(LoggedPayment.SavedOnPhone("c1"))
        editor.onFieldChanged(PaymentEditorField.Amount, "1,500.00")
        editor.onSaveClicked()
        runCurrent()
    }

    @Test
    fun goingOfflineShowsTheBannerAndHidesTheLateFeeOffer() =
        runTest {
            repository.entry.value = synced(paymentOverview())
            val editor = openEditor()
            assertNotNull(editor.uiState.value.offeredLateFee)

            goOffline()
            runCurrent()

            val state = editor.uiState.value
            assertEquals(UiText.Resource(Res.string.offline_banner), state.offlineBanner)
            assertNull(state.offeredLateFee)
            assertNull(state.shownMonthChanges)
        }

    @Test
    fun comingBackOnlineBringsTheLateFeeOfferBack() =
        runTest {
            repository.entry.value = synced(paymentOverview())
            val editor = openEditor()
            goOffline()
            runCurrent()

            repository.freshness.value = Freshness.Online
            runCurrent()

            assertNull(editor.uiState.value.offlineBanner)
            assertNotNull(editor.uiState.value.offeredLateFee)
        }

    @Test
    fun aCheckedLateFeeIsNotSentWhileOffline() =
        runTest {
            repository.entry.value = synced(paymentOverview())
            val editor = openEditor()
            editor.onFieldChanged(PaymentEditorField.Amount, "1,500.00")
            editor.onLateFeeToggled(true)
            goOffline()
            runCurrent()

            editor.onSaveClicked()
            runCurrent()

            assertNull((repository.calls.single() as PaymentRepositoryCall.Log).lateFee)
        }

    @Test
    fun aPaymentSavedOnThePhoneClosesWithTheMessageWhenNoExplanationIsDue() =
        runTest {
            repository.entry.value = synced(paymentOverview())
            val editor = openEditor()
            goOffline()

            editor.events.test {
                saveOnPhone(editor)

                assertEquals(PaymentEditorEvent.PaymentSaved, awaitItem())
            }
            assertEquals(listOf(savedOnPhoneMessage), messages.shown)
            assertFalse(editor.uiState.value.isSyncAlertsExplainerShown)
        }

    @Test
    fun anOnlineSaveShowsNoMessageAndNoExplainer() =
        runTest {
            syncAlerts.isExplanationDue = true
            repository.entry.value = synced(paymentOverview())
            val editor = openEditor()
            editor.onFieldChanged(PaymentEditorField.Amount, "1,500.00")

            editor.events.test {
                editor.onSaveClicked()
                runCurrent()

                assertEquals(PaymentEditorEvent.PaymentSaved, awaitItem())
            }
            assertTrue(messages.shown.isEmpty())
            assertFalse(editor.uiState.value.isSyncAlertsExplainerShown)
        }

    @Test
    fun theExplainerWaitsForAnAnswerBeforeTheEditorCloses() =
        runTest {
            syncAlerts.isExplanationDue = true
            repository.entry.value = synced(paymentOverview())
            val editor = openEditor()
            goOffline()

            editor.events.test {
                saveOnPhone(editor)

                assertTrue(editor.uiState.value.isSyncAlertsExplainerShown)
                assertTrue(messages.shown.isEmpty())
                expectNoEvents()
            }
        }

    @Test
    fun allowingSyncAlertsRemembersTheExplanationAsksForPermissionAndCloses() =
        runTest {
            syncAlerts.isExplanationDue = true
            repository.entry.value = synced(paymentOverview())
            val editor = openEditor()
            goOffline()
            saveOnPhone(editor)

            editor.events.test {
                editor.onSyncAlertsAnswered(SyncAlertsAnswer.Allow)

                assertEquals(PaymentEditorEvent.PaymentSaved, awaitItem())
            }
            assertEquals(1, syncAlerts.explainedCount)
            assertEquals(1, syncAlerts.permissionRequestCount)
            assertEquals(listOf(savedOnPhoneMessage), messages.shown)
            assertFalse(editor.uiState.value.isSyncAlertsExplainerShown)
        }

    @Test
    fun decliningSyncAlertsRemembersTheExplanationWithoutAskingForPermissionAndCloses() =
        runTest {
            syncAlerts.isExplanationDue = true
            repository.entry.value = synced(paymentOverview())
            val editor = openEditor()
            goOffline()
            saveOnPhone(editor)

            editor.events.test {
                editor.onSyncAlertsAnswered(SyncAlertsAnswer.NotNow)

                assertEquals(PaymentEditorEvent.PaymentSaved, awaitItem())
            }
            assertEquals(1, syncAlerts.explainedCount)
            assertEquals(0, syncAlerts.permissionRequestCount)
            assertEquals(listOf(savedOnPhoneMessage), messages.shown)
        }

    @Test
    fun aPendingPaymentOpensPrefilledWithoutTheLateFeeOffer() =
        runTest {
            repository.pendingPayment.value = PendingPayment("Apt 3B", pendingDraft)

            val state = openEditor(pendingClientId = "c1").uiState.value

            assertFalse(state.isLoading)
            assertFalse(state.isNewPayment)
            assertEquals("Apt 3B", state.header?.tenantName)
            assertEquals("420.00", state.amount)
            assertEquals(LocalDate(2026, 3, 5), state.date)
            assertEquals(PaymentMethod.Venmo, state.method)
            assertEquals("Half now", state.note)
            assertNull(state.lateFeeOffer)
            assertNull(state.monthChanges)
        }

    @Test
    fun savingAPendingPaymentEditsItOnThePhoneAndCloses() =
        runTest {
            repository.pendingPayment.value = PendingPayment("Apt 3B", pendingDraft)
            val editor = openEditor(pendingClientId = "c1")
            editor.onFieldChanged(PaymentEditorField.Amount, "450.00")

            editor.events.test {
                editor.onSaveClicked()
                runCurrent()

                assertEquals(PaymentEditorEvent.PaymentSaved, awaitItem())
            }
            val expected = PaymentRepositoryCall.EditPending("c1", pendingDraft.copy(amountCents = 45_000))
            assertEquals(listOf<PaymentRepositoryCall>(expected), repository.calls)
            assertTrue(messages.shown.isEmpty())
        }

    @Test
    fun aPendingPaymentThatSyncedMeanwhileShowsNotFound() =
        runTest {
            val state = openEditor(pendingClientId = "c1").uiState.value

            assertFalse(state.isLoading)
            assertEquals(UiText.Resource(Res.string.entry_error_not_found), state.error)
        }

    @Test
    fun aFailedPendingEditShowsTheError() =
        runTest {
            repository.pendingPayment.value = PendingPayment("Apt 3B", pendingDraft)
            repository.editPendingResult = Err(PaymentError.NotFound)
            val editor = openEditor(pendingClientId = "c1")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Resource(Res.string.entry_error_not_found), editor.uiState.value.error)
        }
}
