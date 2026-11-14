package net.neednot.renttracker.feature.payments.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PaymentRepositoryCall
import net.neednot.renttracker.feature.payments.domain.paymentOverview
import net.neednot.renttracker.feature.payments.domain.savedPayment
import net.neednot.renttracker.feature.payments.domain.synced
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateMatchUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.LateFeeOfferUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_owes
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentEditorViewModelTest : MainDispatcherTest() {
    private val repository = FakePaymentRepository()

    private fun TestScope.openEditor(paymentId: String? = null): PaymentEditorViewModel {
        val editor = PaymentEditorViewModel(tenantId = "t1", paymentId = paymentId, paymentRepository = repository)
        runCurrent()
        return editor
    }

    private fun showOverview(
        overview: net.neednot.renttracker.feature.payments.domain.PaymentEntryOverview = paymentOverview(),
    ) {
        repository.entry.value = synced(overview)
    }

    private fun TestScope.enterAmount(
        editor: PaymentEditorViewModel,
        amount: String,
    ) {
        editor.onFieldChanged(PaymentEditorField.Amount, amount)
        runCurrent()
    }

    private val loggedDraft =
        PaymentDraft(
            amountCents = 150_000,
            dateReceived = LocalDate(2026, 3, 10),
            method = PaymentMethod.Zelle,
            note = null,
        )

    @Test
    fun aNewPaymentStartsToday_withTheTenantsLastMethodAndTheBalanceInWords() =
        runTest {
            showOverview()

            val state = openEditor().uiState.value

            assertFalse(state.isLoading)
            assertEquals(LocalDate(2026, 3, 10), state.date)
            assertEquals(PaymentMethod.Zelle, state.method)
            assertEquals(PaymentMethod.Zelle, state.lastUsedMethod)
            assertEquals("", state.amount)
            assertEquals("Apt 3B", state.header?.tenantName)
            assertEquals("1st Street", state.header?.groupName)
            assertEquals(UiText.Resource(Res.string.balance_owes, listOf("$550.00")), state.header?.balance)
        }

    @Test
    fun theLateFeeBoxIsOfferedWithTheSuggestedAmountUnchecked() =
        runTest {
            showOverview()

            val offer = openEditor().uiState.value.lateFeeOffer

            assertEquals(LateFeeOfferUiState(YearMonth(2026, 3), isChecked = false, amount = "50.00"), offer)
        }

    @Test
    fun noLateFeeBoxWhenThereIsNoSuggestionLeft() =
        runTest {
            showOverview(paymentOverview(lateFeeSuggestion = null))

            assertNull(openEditor().uiState.value.lateFeeOffer)
        }

    @Test
    fun noLateFeeBoxWhenTheCallerMayNotAddDebts() =
        runTest {
            showOverview(paymentOverview(canAddDebts = false))

            assertNull(openEditor().uiState.value.lateFeeOffer)
        }

    @Test
    fun anExistingPaymentLoadsIntoTheFormWithoutLateFeeBoxOrLastUsedLegend() =
        runTest {
            showOverview(paymentOverview(payment = savedPayment()))

            val state = openEditor(paymentId = "p1").uiState.value

            assertFalse(state.isNewPayment)
            assertEquals("1,500.00", state.amount)
            assertEquals(LocalDate(2026, 3, 2), state.date)
            assertEquals(PaymentMethod.Check, state.method)
            assertNull(state.lastUsedMethod)
            assertEquals("March rent", state.note)
            assertNull(state.lateFeeOffer)
        }

    @Test
    fun tappingTheSelectedMethodClearsIt() =
        runTest {
            showOverview()
            val editor = openEditor()

            editor.onMethodClicked(PaymentMethod.Zelle)
            assertNull(editor.uiState.value.method)

            editor.onMethodClicked(PaymentMethod.Cash)
            assertEquals(PaymentMethod.Cash, editor.uiState.value.method)
        }

    @Test
    fun anAmountThatIsNotDollarsAndCentsBlocksSaving() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "12.345")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(
                mapOf(PaymentEditorField.Amount to UiText.Resource(Res.string.entry_error_amount_format)),
                editor.uiState.value.fieldErrors,
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun anAmountOfZeroBlocksSaving() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "0.00")

            editor.onSaveClicked()

            assertEquals(
                UiText.Resource(Res.string.common_error_amount_positive),
                editor.uiState.value.fieldErrors[PaymentEditorField.Amount],
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun aFormatErrorClearsWhenTheFieldIsEdited() =
        runTest {
            showOverview()
            val editor = openEditor()
            editor.onSaveClicked()

            editor.onFieldChanged(PaymentEditorField.Amount, "1")

            assertTrue(
                editor.uiState.value.fieldErrors
                    .isEmpty(),
            )
        }

    @Test
    fun aDateAfterTodayIsBlocked() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "1,500.00")

            editor.onDateChanged(LocalDate(2026, 3, 14))
            editor.onSaveClicked()
            runCurrent()

            val expected = UiText.Resource(Res.string.payment_editor_error_date_future, listOf("Mar 10, 2026"))
            assertEquals(expected, editor.uiState.value.fieldErrors[PaymentEditorField.Date])
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun todayIsAllowedAndPickingAnEarlierDateClearsTheDateError() =
        runTest {
            showOverview()
            val editor = openEditor()
            editor.onDateChanged(LocalDate(2026, 3, 14))

            editor.onDateChanged(LocalDate(2026, 3, 10))

            assertNull(editor.uiState.value.fieldErrors[PaymentEditorField.Date])
        }

    @Test
    fun savingShowsTheSavingStateWhileTheCallIsInFlight() =
        runTest {
            showOverview()
            repository.saveGate = CompletableDeferred()
            val editor = openEditor()
            enterAmount(editor, "1,500.00")

            editor.onSaveClicked()
            runCurrent()
            assertTrue(editor.uiState.value.isSaving)

            editor.onSaveClicked()
            runCurrent()
            assertEquals(1, repository.calls.size)

            repository.saveGate?.complete(Unit)
            runCurrent()
            assertFalse(editor.uiState.value.isSaving)
        }

    @Test
    fun aValidPaymentIsLoggedWithoutLateFeeAndNavigatesBack() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "1,500.00")

            editor.events.test {
                editor.onSaveClicked()
                runCurrent()

                assertEquals(PaymentEditorEvent.PaymentSaved, awaitItem())
            }
            val call = repository.calls.single() as PaymentRepositoryCall.Log
            assertEquals("t1", call.tenantId)
            assertEquals(loggedDraft, call.draft)
            assertNull(call.lateFee)
            assertEquals(DuplicatePolicy.AskFirst, call.duplicatePolicy)
        }

    @Test
    fun theNoteIsTrimmedAndAnEmptyNoteIsLeftOut() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "10")
            editor.onFieldChanged(PaymentEditorField.Note, "  Cash in mailbox  ")
            editor.onSaveClicked()
            runCurrent()

            assertEquals("Cash in mailbox", (repository.calls.single() as PaymentRepositoryCall.Log).draft.note)
        }

    @Test
    fun aCheckedLateFeeIsSavedWithThePaymentUsingTheEditedAmount() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "1,500.00")
            editor.onLateFeeToggled(true)
            editor.onFieldChanged(PaymentEditorField.LateFeeAmount, "45.00")

            editor.onSaveClicked()
            runCurrent()

            val call = repository.calls.single() as PaymentRepositoryCall.Log
            assertEquals(LateFeeDraft(amountCents = 4_500, forMonth = YearMonth(2026, 3)), call.lateFee)
        }

    @Test
    fun anUncheckedLateFeeIsNotSent() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "1,500.00")
            editor.onLateFeeToggled(true)
            editor.onLateFeeToggled(false)

            editor.onSaveClicked()
            runCurrent()

            assertNull((repository.calls.single() as PaymentRepositoryCall.Log).lateFee)
        }

    @Test
    fun aBadLateFeeAmountBlocksSavingNextToThatField() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "1,500.00")
            editor.onLateFeeToggled(true)
            editor.onFieldChanged(PaymentEditorField.LateFeeAmount, "abc")

            editor.onSaveClicked()

            assertEquals(
                UiText.Resource(Res.string.entry_error_amount_format),
                editor.uiState.value.fieldErrors[PaymentEditorField.LateFeeAmount],
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun aPossibleDuplicateAsksBeforeSavingAndShowsTheMatchWithYouForTheCallersOwnEntry() =
        runTest {
            showOverview()
            val match =
                DuplicateMatch(
                    DuplicateKind.Payment,
                    LocalDate(2026, 3, 2),
                    150_000,
                    PaymentMethod.Zelle,
                    "Dana",
                    "account",
                )
            repository.logResult = Err(PaymentError.PossibleDuplicate(listOf(match)))
            val editor = openEditor()
            enterAmount(editor, "1,500.00")

            editor.onSaveClicked()
            runCurrent()

            val dialog = checkNotNull(editor.uiState.value.duplicate)
            assertEquals(UiText.Resource(Res.string.payment_duplicate_title_one), dialog.title)
            assertEquals(UiText.Resource(Res.string.payment_duplicate_intro_payment), dialog.intro)
            assertEquals(UiText.Resource(Res.string.payment_duplicate_question_one), dialog.question)
            assertEquals(
                listOf(
                    DuplicateMatchUiState(
                        kind = null,
                        date = "Mar 2",
                        method = PaymentMethod.Zelle,
                        by = UiText.Resource(Res.string.payment_duplicate_you),
                        amount = "$1,500.00",
                    ),
                ),
                dialog.matches,
            )
            assertNull(editor.uiState.value.error)
            assertFalse(editor.uiState.value.isSaving)
        }

    @Test
    fun aDuplicateByAnotherMemberShowsTheirName() =
        runTest {
            showOverview()
            val match =
                DuplicateMatch(
                    DuplicateKind.Payment,
                    LocalDate(2026, 3, 2),
                    150_000,
                    null,
                    "Sam Rivera",
                    "sam-account",
                )
            repository.logResult = Err(PaymentError.PossibleDuplicate(listOf(match)))
            val editor = openEditor()
            enterAmount(editor, "1,500.00")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(
                UiText.Plain("Sam Rivera"),
                editor.uiState.value.duplicate
                    ?.matches
                    ?.single()
                    ?.by,
            )
        }

    @Test
    fun aPaymentAndLateFeeDuplicateAreListedTogetherUnderOneQuestion() =
        runTest {
            showOverview()
            val matches =
                listOf(
                    DuplicateMatch(
                        DuplicateKind.Payment,
                        LocalDate(2026, 3, 2),
                        150_000,
                        PaymentMethod.Check,
                        "Sam Rivera",
                        "sam-account",
                    ),
                    DuplicateMatch(
                        DuplicateKind.LateFee,
                        LocalDate(2026, 3, 8),
                        5_000,
                        null,
                        "Sam Rivera",
                        "sam-account",
                    ),
                )
            repository.logResult = Err(PaymentError.PossibleDuplicate(matches))
            val editor = openEditor()
            enterAmount(editor, "1,500.00")
            editor.onLateFeeToggled(true)

            editor.onSaveClicked()
            runCurrent()

            val dialog = checkNotNull(editor.uiState.value.duplicate)
            assertEquals(UiText.Resource(Res.string.payment_duplicate_title_many), dialog.title)
            assertEquals(UiText.Resource(Res.string.payment_duplicate_intro_both), dialog.intro)
            assertEquals(UiText.Resource(Res.string.payment_duplicate_question_both), dialog.question)
            assertEquals(
                listOf(
                    UiText.Resource(Res.string.payment_duplicate_kind_payment),
                    UiText.Resource(Res.string.payment_duplicate_kind_late_fee, listOf("March")),
                ),
                dialog.matches.map { it.kind },
            )
        }

    @Test
    fun savingAnywayResendsTheSameRequestWithTheSameClientIdAndConfirmation() =
        runTest {
            showOverview()
            val match =
                DuplicateMatch(DuplicateKind.Payment, LocalDate(2026, 3, 2), 150_000, null, "Sam", "sam-account")
            repository.logResult = Err(PaymentError.PossibleDuplicate(listOf(match)))
            val editor = openEditor()
            enterAmount(editor, "1,500.00")
            editor.onSaveClicked()
            runCurrent()
            repository.logResult =
                com.github.michaelbull.result
                    .Ok("p9")

            editor.events.test {
                editor.onDuplicateConfirmed()
                runCurrent()

                assertEquals(PaymentEditorEvent.PaymentSaved, awaitItem())
            }
            val (first, second) = repository.calls.map { it as PaymentRepositoryCall.Log }
            assertEquals(DuplicatePolicy.AskFirst, first.duplicatePolicy)
            assertEquals(DuplicatePolicy.SaveAnyway, second.duplicatePolicy)
            assertEquals(first.clientId, second.clientId)
            assertEquals(first.draft, second.draft)
            assertNull(editor.uiState.value.duplicate)
        }

    @Test
    fun goingBackFromTheDuplicateDialogKeepsTheFormAndSavesNothing() =
        runTest {
            showOverview()
            val match =
                DuplicateMatch(DuplicateKind.Payment, LocalDate(2026, 3, 2), 150_000, null, "Sam", "sam-account")
            repository.logResult = Err(PaymentError.PossibleDuplicate(listOf(match)))
            val editor = openEditor()
            enterAmount(editor, "1,500.00")
            editor.onSaveClicked()
            runCurrent()

            editor.onDuplicateDismissed()

            assertNull(editor.uiState.value.duplicate)
            assertEquals("1,500.00", editor.uiState.value.amount)
            assertEquals(1, repository.calls.size)
        }

    @Test
    fun savingWithoutAConnectionExplainsNothingWasSaved() =
        runTest {
            showOverview()
            repository.logResult = Err(PaymentError.Offline)
            val editor = openEditor()
            enterAmount(editor, "1,500.00")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Resource(Res.string.payment_editor_error_offline), editor.uiState.value.error)
            assertFalse(editor.uiState.value.isSaving)
        }

    @Test
    fun retryingAfterOfflineReusesTheClientId() =
        runTest {
            showOverview()
            repository.logResult = Err(PaymentError.Offline)
            val editor = openEditor()
            enterAmount(editor, "1,500.00")
            editor.onSaveClicked()
            runCurrent()

            editor.onSaveClicked()
            runCurrent()

            val (first, second) = repository.calls.map { it as PaymentRepositoryCall.Log }
            assertEquals(first.clientId, second.clientId)
        }

    @Test
    fun aServerValidationMessageIsShownAsIs() =
        runTest {
            showOverview()
            repository.logResult = Err(PaymentError.ValidationFailed("Amount is too large."))
            val editor = openEditor()
            enterAmount(editor, "1,500.00")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Plain("Amount is too large."), editor.uiState.value.error)
        }

    @Test
    fun editingSendsTheLoadedVersionAndTheWholeForm() =
        runTest {
            showOverview(paymentOverview(payment = savedPayment(version = 2)))
            val editor = openEditor(paymentId = "p1")
            enterAmount(editor, "1,450.00")

            editor.events.test {
                editor.onSaveClicked()
                runCurrent()

                assertEquals(PaymentEditorEvent.PaymentSaved, awaitItem())
            }
            val expected =
                PaymentRepositoryCall.Edit(
                    paymentId = "p1",
                    expectedVersion = 2,
                    draft = PaymentDraft(145_000, LocalDate(2026, 3, 2), PaymentMethod.Check, "March rent"),
                )
            assertEquals(listOf<PaymentRepositoryCall>(expected), repository.calls)
        }

    @Test
    fun anEditConflictShowsWhoChangedItAndReloadsTheFormFromTheNewValues() =
        runTest {
            showOverview(paymentOverview(payment = savedPayment(version = 2)))
            val editor = openEditor(paymentId = "p1")
            enterAmount(editor, "1,450.00")
            repository.entry.value =
                synced(
                    paymentOverview(
                        payment = savedPayment(version = 3, dateReceived = LocalDate(2026, 3, 3), note = "short $50"),
                    ),
                )
            repository.editResult = Err(PaymentError.EditConflict("Sam changed this payment.", "Sam Rivera"))

            editor.onSaveClicked()
            runCurrent()

            val state = editor.uiState.value
            assertEquals(
                UiText.Resource(Res.string.payment_editor_conflict_named, listOf("Sam Rivera")),
                state.conflictBanner,
            )
            assertEquals("1,500.00", state.amount)
            assertEquals(LocalDate(2026, 3, 3), state.date)
            assertEquals("short $50", state.note)
            assertFalse(state.isSaving)

            repository.editResult =
                com.github.michaelbull.result
                    .Ok(Unit)
            editor.onSaveClicked()
            runCurrent()
            assertEquals(3, (repository.calls.last() as PaymentRepositoryCall.Edit).expectedVersion)
            assertNull(editor.uiState.value.conflictBanner)
        }

    @Test
    fun aMissingPaymentShowsNotFoundInsteadOfAForm() =
        runTest {
            repository.entry.value = synced(paymentOverview(payment = null))

            val state = openEditor(paymentId = "p1").uiState.value

            assertFalse(state.isLoading)
            assertTrue(state.error != null)
            assertNull(state.header)
        }
}
