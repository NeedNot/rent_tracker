package net.neednot.renttracker.feature.payments.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.domain.DuplicateKind
import net.neednot.renttracker.feature.payments.domain.DuplicateMatch
import net.neednot.renttracker.feature.payments.domain.DuplicatePolicy
import net.neednot.renttracker.feature.payments.domain.FakePaymentRepository
import net.neednot.renttracker.feature.payments.domain.LateFeeDraft
import net.neednot.renttracker.feature.payments.domain.LateFeeSuggestion
import net.neednot.renttracker.feature.payments.domain.MonthState
import net.neednot.renttracker.feature.payments.domain.MonthStateChange
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentEntryOverview
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PaymentRepositoryCall
import net.neednot.renttracker.feature.payments.domain.paymentOverview
import net.neednot.renttracker.feature.payments.domain.savedPayment
import net.neednot.renttracker.feature.payments.domain.synced
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateMatchUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.LateFeeOfferUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.MonthChangeLine
import net.neednot.renttracker.feature.payments.presentation.uistate.MonthChangesUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.calendar_state_owed
import net.neednot.renttracker.resources.calendar_state_paid
import net.neednot.renttracker.resources.calendar_state_partial
import net.neednot.renttracker.resources.common_error_amount_positive
import net.neednot.renttracker.resources.entry_error_amount_format
import net.neednot.renttracker.resources.payment_duplicate_intro_both
import net.neednot.renttracker.resources.payment_duplicate_intro_payment
import net.neednot.renttracker.resources.payment_duplicate_kind_late_fee
import net.neednot.renttracker.resources.payment_duplicate_kind_payment
import net.neednot.renttracker.resources.payment_duplicate_question_both
import net.neednot.renttracker.resources.payment_duplicate_question_one
import net.neednot.renttracker.resources.payment_duplicate_title_many
import net.neednot.renttracker.resources.payment_duplicate_title_one
import net.neednot.renttracker.resources.payment_duplicate_you
import net.neednot.renttracker.resources.payment_editor_conflict_named
import net.neednot.renttracker.resources.payment_editor_error_date_future
import net.neednot.renttracker.resources.payment_editor_error_offline
import net.neednot.renttracker.resources.payment_preview_heading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentEditorPreviewTest : MainDispatcherTest() {
    private val repository = FakePaymentRepository()

    private fun TestScope.openEditor(paymentId: String? = null): PaymentEditorViewModel {
        val editor = PaymentEditorViewModel(tenantId = "t1", paymentId = paymentId, paymentRepository = repository)
        runCurrent()
        return editor
    }

    private fun TestScope.enterAmount(
        editor: PaymentEditorViewModel,
        amount: String,
    ) {
        editor.onFieldChanged(PaymentEditorField.Amount, amount)
        runCurrent()
    }

    private fun showOverview(overview: PaymentEntryOverview = paymentOverview()) {
        repository.entry.value = synced(overview)
    }

    private val febChange = MonthStateChange(YearMonth(2026, 2), MonthState.Paid, MonthState.Partial)
    private val marChange = MonthStateChange(YearMonth(2026, 3), MonthState.Owed, MonthState.Paid)

    private fun TestScope.openSavedPayment(): PaymentEditorViewModel {
        showOverview(paymentOverview(payment = savedPayment()))
        return openEditor(paymentId = "p1")
    }

    private fun TestScope.settlePreview() {
        advanceTimeBy(400)
        runCurrent()
    }

    private fun previews() = repository.calls.filterIsInstance<PaymentRepositoryCall.Preview>()

    @Test
    fun noPreviewIsRequestedWhileTheFormMatchesTheSavedPayment() =
        runTest {
            val editor = openSavedPayment()
            settlePreview()

            assertEquals(emptyList(), previews())
            assertNull(editor.uiState.value.monthChanges)
        }

    @Test
    fun noPreviewIsRequestedForAnInvalidAmountOrForANewPayment() =
        runTest {
            val editor = openSavedPayment()
            enterAmount(editor, "abc")
            settlePreview()
            showOverview()
            val newEditor = openEditor()
            enterAmount(newEditor, "900.00")
            settlePreview()

            assertEquals(emptyList(), previews())
        }

    @Test
    fun aChangedValidFormAsksForAPreviewAfterTheDebounce() =
        runTest {
            val editor = openSavedPayment()
            enterAmount(editor, "1,000.00")
            advanceTimeBy(100)
            runCurrent()
            assertEquals(emptyList(), previews())

            settlePreview()

            assertEquals(
                listOf(PaymentRepositoryCall.Preview("t1", "p1", 100_000, LocalDate(2026, 3, 2))),
                previews(),
            )
        }

    @Test
    fun typingQuicklyOnlyAsksForTheLastValue() =
        runTest {
            val editor = openSavedPayment()
            enterAmount(editor, "1")
            enterAmount(editor, "10")
            enterAmount(editor, "1,000.00")
            settlePreview()

            assertEquals(listOf(100_000L), previews().map { it.amountCents })
        }

    @Test
    fun changedMonthsAreShownOneLinePerMonthInWords() =
        runTest {
            repository.previewResult =
                com.github.michaelbull.result
                    .Ok(listOf(febChange, marChange))
            val editor = openSavedPayment()
            enterAmount(editor, "1,000.00")
            settlePreview()

            assertEquals(
                MonthChangesUiState.Changes(
                    heading = UiText.Plural(Res.plurals.payment_preview_heading, 2, listOf(2)),
                    lines =
                        listOf(
                            MonthChangeLine(
                                "February 2026",
                                UiText.Resource(Res.string.calendar_state_paid),
                                UiText.Resource(Res.string.calendar_state_partial),
                            ),
                            MonthChangeLine(
                                "March 2026",
                                UiText.Resource(Res.string.calendar_state_owed),
                                UiText.Resource(Res.string.calendar_state_paid),
                            ),
                        ),
                ),
                editor.uiState.value.monthChanges,
            )
        }

    @Test
    fun anEmptyPreviewShowsNothing() =
        runTest {
            val editor = openSavedPayment()
            enterAmount(editor, "1,000.00")
            settlePreview()

            assertNull(editor.uiState.value.monthChanges)
        }

    @Test
    fun returningToTheSavedValuesHidesThePreview() =
        runTest {
            repository.previewResult =
                com.github.michaelbull.result
                    .Ok(listOf(febChange))
            val editor = openSavedPayment()
            enterAmount(editor, "1,000.00")
            settlePreview()
            enterAmount(editor, "1,500.00")

            assertNull(editor.uiState.value.monthChanges)
        }

    @Test
    fun aStaleAnswerIsIgnoredWhenTheFormChangedMeanwhile() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            repository.previewGate = gate
            repository.previewResult =
                com.github.michaelbull.result
                    .Ok(listOf(febChange))
            val editor = openSavedPayment()
            enterAmount(editor, "1,000.00")
            settlePreview()

            enterAmount(editor, "1,500.00")
            gate.complete(Unit)
            runCurrent()

            assertNull(editor.uiState.value.monthChanges)
        }

    @Test
    fun aFailedPreviewShowsTheFallbackAndSavingStillWorks() =
        runTest {
            repository.previewResult = Err(PaymentError.Offline)
            val editor = openSavedPayment()
            enterAmount(editor, "1,000.00")
            settlePreview()
            assertEquals(MonthChangesUiState.CheckFailed, editor.uiState.value.monthChanges)

            editor.onSaveClicked()
            runCurrent()

            assertEquals(100_000, (repository.calls.last() as PaymentRepositoryCall.Edit).draft.amountCents)
        }
}
