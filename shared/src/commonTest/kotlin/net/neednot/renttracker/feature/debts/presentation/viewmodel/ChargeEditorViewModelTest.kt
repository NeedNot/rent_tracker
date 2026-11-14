package net.neednot.renttracker.feature.debts.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.domain.DebtError
import net.neednot.renttracker.feature.debts.domain.DebtRepositoryCall
import net.neednot.renttracker.feature.debts.domain.DuplicateLateFee
import net.neednot.renttracker.feature.debts.domain.DuplicatePolicy
import net.neednot.renttracker.feature.debts.domain.FakeDebtRepository
import net.neednot.renttracker.feature.debts.domain.chargeOverview
import net.neednot.renttracker.feature.debts.domain.savedCustom
import net.neednot.renttracker.feature.debts.domain.savedLateFee
import net.neednot.renttracker.feature.debts.domain.synced
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorField
import net.neednot.renttracker.feature.debts.presentation.uistate.DuplicateLateFeeUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.charge_duplicate_intro
import net.neednot.renttracker.resources.charge_duplicate_you
import net.neednot.renttracker.resources.charge_editor_conflict_named
import net.neednot.renttracker.resources.charge_editor_error_note_required
import net.neednot.renttracker.resources.charge_editor_error_offline
import net.neednot.renttracker.resources.input_error_amount_format
import net.neednot.renttracker.resources.input_error_amount_too_large
import net.neednot.renttracker.resources.input_error_too_long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChargeEditorViewModelTest : MainDispatcherTest() {
    private val repository = FakeDebtRepository()

    private fun TestScope.openEditor(
        target: ChargeEditorTarget = ChargeEditorTarget(tenantId = "t1"),
    ): ChargeEditorViewModel {
        val editor = ChargeEditorViewModel(target = target, debtRepository = repository)
        runCurrent()
        return editor
    }

    private fun showOverview(
        overview: net.neednot.renttracker.feature.debts.domain.ChargeEntryOverview = chargeOverview(),
    ) {
        repository.entry.value = synced(overview)
    }

    private fun TestScope.enterAmount(
        editor: ChargeEditorViewModel,
        amount: String,
    ) {
        editor.onFieldChanged(ChargeEditorField.Amount, amount)
        runCurrent()
    }

    private val suggestion = ChargeSuggestion(forMonth = YearMonth(2026, 2), amountCents = 5_000)

    @Test
    fun aNewChargeStartsAsALateFeeDatedTodayForTheCurrentMonth() =
        runTest {
            showOverview()

            val state = openEditor().uiState.value

            assertFalse(state.isLoading)
            assertTrue(state.isNewCharge)
            assertEquals(DebtKind.LateFee, state.kind)
            assertEquals(LocalDate(2026, 3, 10), state.date)
            assertEquals(YearMonth(2026, 3), state.forMonth)
            assertEquals("", state.amount)
            assertFalse(state.isFromSuggestion)
            assertEquals(UiText.Resource(Res.string.balance_owes, listOf("$550.00")), state.header?.balance)
        }

    @Test
    fun aSuggestionFromTheRoutePrefillsTheAmountAndMonth() =
        runTest {
            showOverview()

            val state = openEditor(ChargeEditorTarget("t1", suggestion = suggestion)).uiState.value

            assertEquals(DebtKind.LateFee, state.kind)
            assertEquals("50.00", state.amount)
            assertEquals(YearMonth(2026, 2), state.forMonth)
            assertTrue(state.isFromSuggestion)
        }

    @Test
    fun aSuggestedLateFeeIsSavedWithTheSuggestedValuesUnlessChanged() =
        runTest {
            showOverview()
            val editor = openEditor(ChargeEditorTarget("t1", suggestion = suggestion))

            editor.onSaveClicked()
            runCurrent()

            val call = repository.calls.single() as DebtRepositoryCall.Add
            val expected = ChargeDraft.LateFee(5_000, LocalDate(2026, 3, 10), YearMonth(2026, 2), note = null)
            assertEquals(expected, call.draft)
            assertEquals(DuplicatePolicy.AskFirst, call.duplicatePolicy)
        }

    @Test
    fun switchingToACustomDebtRequiresANote() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "42.50")
            editor.onKindSelected(DebtKind.Custom)

            editor.onSaveClicked()
            runCurrent()

            assertEquals(
                mapOf(ChargeEditorField.Note to UiText.Resource(Res.string.charge_editor_error_note_required)),
                editor.uiState.value.fieldErrors,
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun aCustomDebtIsSavedWithItsNoteAndNoMonth() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "42.50")
            editor.onKindSelected(DebtKind.Custom)
            editor.onFieldChanged(ChargeEditorField.Note, "  Water bill, Feb ")

            editor.onSaveClicked()
            runCurrent()

            val call = repository.calls.single() as DebtRepositoryCall.Add
            assertEquals(ChargeDraft.Custom(4_250, LocalDate(2026, 3, 10), "Water bill, Feb"), call.draft)
        }

    @Test
    fun aLateFeeNeedsNoNote() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "50")

            editor.onSaveClicked()
            runCurrent()

            val draft = (repository.calls.single() as DebtRepositoryCall.Add).draft
            assertEquals(ChargeDraft.LateFee(5_000, LocalDate(2026, 3, 10), YearMonth(2026, 3), note = null), draft)
        }

    @Test
    fun theMonthPickedForALateFeeIsSent() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "50")
            editor.onForMonthChanged(YearMonth(2026, 1))

            editor.onSaveClicked()
            runCurrent()

            assertEquals(
                YearMonth(2026, 1),
                ((repository.calls.single() as DebtRepositoryCall.Add).draft as ChargeDraft.LateFee).forMonth,
            )
        }

    @Test
    fun aCustomNoteOverOneHundredCharactersIsFlaggedAsTyped() =
        runTest {
            showOverview()
            val editor = openEditor()
            editor.onKindSelected(DebtKind.Custom)

            editor.onFieldChanged(ChargeEditorField.Note, "x".repeat(101))

            assertEquals(
                UiText.Resource(Res.string.input_error_too_long, listOf(100)),
                editor.uiState.value.fieldErrors[ChargeEditorField.Note],
            )
        }

    @Test
    fun aLateFeeNoteMayRunToTheNotesLimit() =
        runTest {
            showOverview()
            val editor = openEditor()

            editor.onFieldChanged(ChargeEditorField.Note, "x".repeat(2000))
            assertNull(editor.uiState.value.fieldErrors[ChargeEditorField.Note])

            editor.onFieldChanged(ChargeEditorField.Note, "x".repeat(2001))
            assertEquals(
                UiText.Resource(Res.string.input_error_too_long, listOf(2000)),
                editor.uiState.value.fieldErrors[ChargeEditorField.Note],
            )
        }

    @Test
    fun anAmountErrorAppearsAsSoonAsItIsTyped() =
        runTest {
            showOverview()
            val editor = openEditor()

            editor.onFieldChanged(ChargeEditorField.Amount, "5.123")
            assertEquals(
                UiText.Resource(Res.string.input_error_amount_format),
                editor.uiState.value.fieldErrors[ChargeEditorField.Amount],
            )

            editor.onFieldChanged(ChargeEditorField.Amount, "1,000,000.01")
            assertEquals(
                UiText.Resource(Res.string.input_error_amount_too_large),
                editor.uiState.value.fieldErrors[ChargeEditorField.Amount],
            )
        }

    @Test
    fun missingRequiredFieldsAreFlaggedOnlyAfterSaveIsPressed() =
        runTest {
            showOverview()
            val editor = openEditor()
            editor.onKindSelected(DebtKind.Custom)
            editor.onFieldChanged(ChargeEditorField.Note, "x")
            editor.onFieldChanged(ChargeEditorField.Note, "")
            assertTrue(
                editor.uiState.value.fieldErrors
                    .isEmpty(),
            )

            editor.onSaveClicked()

            assertEquals(
                mapOf(
                    ChargeEditorField.Amount to UiText.Resource(Res.string.input_error_amount_format),
                    ChargeEditorField.Note to UiText.Resource(Res.string.charge_editor_error_note_required),
                ),
                editor.uiState.value.fieldErrors,
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun aServerValidationFailureNamingAFieldShowsUnderThatField() =
        runTest {
            showOverview()
            repository.addResult = Err(DebtError.ValidationFailed("Note is required.", "Note"))
            val editor = openEditor()
            enterAmount(editor, "50")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(
                mapOf(ChargeEditorField.Note to UiText.Plain("Note is required.")),
                editor.uiState.value.fieldErrors,
            )
            assertNull(editor.uiState.value.error)
        }

    @Test
    fun aServerValidationFailureWithoutAFieldShowsAtTheTopOfTheForm() =
        runTest {
            showOverview()
            repository.addResult = Err(DebtError.ValidationFailed("Date must be a real date.", "Date"))
            val editor = openEditor()
            enterAmount(editor, "50")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Plain("Date must be a real date."), editor.uiState.value.error)
            assertTrue(
                editor.uiState.value.fieldErrors
                    .isEmpty(),
            )
        }

    @Test
    fun anAmountThatIsNotDollarsAndCentsBlocksSaving() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "5.123")

            editor.onSaveClicked()

            assertEquals(
                mapOf(ChargeEditorField.Amount to UiText.Resource(Res.string.input_error_amount_format)),
                editor.uiState.value.fieldErrors,
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun aFutureDateIsAllowedForACharge() =
        runTest {
            showOverview()
            val editor = openEditor()
            enterAmount(editor, "50")
            editor.onDateChanged(LocalDate(2026, 4, 1))

            editor.onSaveClicked()
            runCurrent()

            assertEquals(LocalDate(2026, 4, 1), (repository.calls.single() as DebtRepositoryCall.Add).draft.date)
        }

    @Test
    fun savingShowsTheSavingStateAndNavigatesBackOnSuccess() =
        runTest {
            showOverview()
            repository.saveGate = CompletableDeferred()
            val editor = openEditor()
            enterAmount(editor, "50")

            editor.events.test {
                editor.onSaveClicked()
                runCurrent()
                assertTrue(editor.uiState.value.isSaving)

                editor.onSaveClicked()
                runCurrent()
                assertEquals(1, repository.calls.size)

                repository.saveGate?.complete(Unit)
                runCurrent()

                assertEquals(ChargeEditorEvent.ChargeSaved, awaitItem())
            }
            assertFalse(editor.uiState.value.isSaving)
        }

    @Test
    fun anExistingLateFeeLoadsWithItsTypeFixed() =
        runTest {
            showOverview(chargeOverview(charge = savedLateFee()))

            val editor = openEditor(ChargeEditorTarget("t1", debtId = "d1"))
            editor.onKindSelected(DebtKind.Custom)
            val state = editor.uiState.value

            assertFalse(state.isNewCharge)
            assertEquals(DebtKind.LateFee, state.kind)
            assertEquals("50.00", state.amount)
            assertEquals(LocalDate(2026, 2, 6), state.date)
            assertEquals(YearMonth(2026, 2), state.forMonth)
            assertEquals("Paid Feb 4, after grace", state.note)
            assertFalse(state.isFromSuggestion)
        }

    @Test
    fun editingACustomDebtKeepsItCustomAndRequiresTheNote() =
        runTest {
            showOverview(chargeOverview(charge = savedCustom()))
            val editor = openEditor(ChargeEditorTarget("t1", debtId = "d1"))
            editor.onFieldChanged(ChargeEditorField.Note, "")

            editor.onSaveClicked()

            assertTrue(
                editor.uiState.value.fieldErrors
                    .containsKey(ChargeEditorField.Note),
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun editingSendsTheLoadedVersionAndTheWholeForm() =
        runTest {
            showOverview(chargeOverview(charge = savedLateFee(version = 2)))
            val editor = openEditor(ChargeEditorTarget("t1", debtId = "d1"))
            enterAmount(editor, "60.00")

            editor.events.test {
                editor.onSaveClicked()
                runCurrent()

                assertEquals(ChargeEditorEvent.ChargeSaved, awaitItem())
            }
            val expected =
                DebtRepositoryCall.Edit(
                    debtId = "d1",
                    expectedVersion = 2,
                    draft =
                        ChargeDraft.LateFee(
                            6_000,
                            LocalDate(2026, 2, 6),
                            YearMonth(2026, 2),
                            "Paid Feb 4, after grace",
                        ),
                )
            assertEquals(listOf<DebtRepositoryCall>(expected), repository.calls)
        }

    @Test
    fun aDuplicateLateFeeAsksBeforeAddingAndNamesTheMonthAndWhoAddedIt() =
        runTest {
            showOverview()
            val match = DuplicateLateFee(LocalDate(2026, 3, 8), 5_000, "Sam Rivera", "sam-account")
            repository.addResult = Err(DebtError.PossibleDuplicate(listOf(match)))
            val editor = openEditor()
            enterAmount(editor, "50")

            editor.onSaveClicked()
            runCurrent()

            val dialog = checkNotNull(editor.uiState.value.duplicate)
            assertEquals(UiText.Resource(Res.string.charge_duplicate_intro, listOf("March")), dialog.intro)
            assertEquals(
                listOf(DuplicateLateFeeUiState("Mar 8", UiText.Plain("Sam Rivera"), "$50.00")),
                dialog.matches,
            )
        }

    @Test
    fun aDuplicateAddedByTheCallerShowsYou() =
        runTest {
            showOverview()
            val match = DuplicateLateFee(LocalDate(2026, 3, 8), 5_000, "Dana", "account")
            repository.addResult = Err(DebtError.PossibleDuplicate(listOf(match)))
            val editor = openEditor()
            enterAmount(editor, "50")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(
                UiText.Resource(Res.string.charge_duplicate_you),
                editor.uiState.value.duplicate
                    ?.matches
                    ?.single()
                    ?.by,
            )
        }

    @Test
    fun aDuplicateByAnotherMemberWithTheCallersNameShowsTheirName() =
        runTest {
            showOverview()
            val match = DuplicateLateFee(LocalDate(2026, 3, 8), 5_000, "Dana", "other-dana-account")
            repository.addResult = Err(DebtError.PossibleDuplicate(listOf(match)))
            val editor = openEditor()
            enterAmount(editor, "50")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(
                UiText.Plain("Dana"),
                editor.uiState.value.duplicate
                    ?.matches
                    ?.single()
                    ?.by,
            )
        }

    @Test
    fun addingAnywayResendsTheSameRequestWithTheSameClientId() =
        runTest {
            showOverview()
            val match = DuplicateLateFee(LocalDate(2026, 3, 8), 5_000, "Sam Rivera", "sam-account")
            repository.addResult = Err(DebtError.PossibleDuplicate(listOf(match)))
            val editor = openEditor()
            enterAmount(editor, "50")
            editor.onSaveClicked()
            runCurrent()
            repository.addResult = Ok("d9")

            editor.events.test {
                editor.onDuplicateConfirmed()
                runCurrent()

                assertEquals(ChargeEditorEvent.ChargeSaved, awaitItem())
            }
            val (first, second) = repository.calls.map { it as DebtRepositoryCall.Add }
            assertEquals(DuplicatePolicy.AskFirst, first.duplicatePolicy)
            assertEquals(DuplicatePolicy.AddAnyway, second.duplicatePolicy)
            assertEquals(first.clientId, second.clientId)
            assertEquals(first.draft, second.draft)
        }

    @Test
    fun goingBackFromTheDuplicateDialogKeepsTheFormAndAddsNothing() =
        runTest {
            showOverview()
            val match = DuplicateLateFee(LocalDate(2026, 3, 8), 5_000, "Sam Rivera", "sam-account")
            repository.addResult = Err(DebtError.PossibleDuplicate(listOf(match)))
            val editor = openEditor()
            enterAmount(editor, "50")
            editor.onSaveClicked()
            runCurrent()

            editor.onDuplicateDismissed()

            assertNull(editor.uiState.value.duplicate)
            assertEquals("50", editor.uiState.value.amount)
            assertEquals(1, repository.calls.size)
        }

    @Test
    fun savingWithoutAConnectionExplainsNothingWasSaved() =
        runTest {
            showOverview()
            repository.addResult = Err(DebtError.Offline)
            val editor = openEditor()
            enterAmount(editor, "50")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Resource(Res.string.charge_editor_error_offline), editor.uiState.value.error)
            assertFalse(editor.uiState.value.isSaving)
        }

    @Test
    fun aServerMessageIsShownAsIs() =
        runTest {
            showOverview()
            repository.addResult = Err(DebtError.TenantArchived("Restore this tenant first."))
            val editor = openEditor()
            enterAmount(editor, "50")

            editor.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Plain("Restore this tenant first."), editor.uiState.value.error)
        }

    @Test
    fun anEditConflictShowsWhoChangedItAndReloadsTheFormFromTheNewValues() =
        runTest {
            showOverview(chargeOverview(charge = savedLateFee(version = 2)))
            val editor = openEditor(ChargeEditorTarget("t1", debtId = "d1"))
            enterAmount(editor, "60.00")
            repository.entry.value = synced(chargeOverview(charge = savedLateFee(version = 3, amountCents = 7_500)))
            repository.editResult = Err(DebtError.EditConflict("Sam changed this charge.", "Sam Rivera"))

            editor.onSaveClicked()
            runCurrent()

            val state = editor.uiState.value
            assertEquals(
                UiText.Resource(Res.string.charge_editor_conflict_named, listOf("Sam Rivera")),
                state.conflictBanner,
            )
            assertEquals("75.00", state.amount)
            assertFalse(state.isSaving)

            repository.editResult = Ok(Unit)
            editor.onSaveClicked()
            runCurrent()
            assertEquals(3, (repository.calls.last() as DebtRepositoryCall.Edit).expectedVersion)
            assertNull(editor.uiState.value.conflictBanner)
        }

    @Test
    fun aMissingChargeShowsAnErrorInsteadOfAForm() =
        runTest {
            repository.entry.value = synced(chargeOverview(charge = null))

            val state = openEditor(ChargeEditorTarget("t1", debtId = "d1")).uiState.value

            assertFalse(state.isLoading)
            assertTrue(state.error != null)
            assertNull(state.header)
        }
}
