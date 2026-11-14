package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.BalanceTotal
import net.neednot.renttracker.feature.tenants.domain.FakeTenantRepository
import net.neednot.renttracker.feature.tenants.domain.RentTermDraft
import net.neednot.renttracker.feature.tenants.domain.RentTermPreview
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantRepositoryCall
import net.neednot.renttracker.feature.tenants.domain.rentTerm
import net.neednot.renttracker.feature.tenants.domain.synced
import net.neednot.renttracker.feature.tenants.domain.tenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.PastMonthWarningUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermDeletionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.input_error_amount_format
import net.neednot.renttracker.resources.rent_term_editor_error_reason_required
import net.neednot.renttracker.resources.tenant_editor_error_due_day
import net.neednot.renttracker.resources.tenant_error_not_found
import net.neednot.renttracker.resources.tenant_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RentTermEditorViewModelTest : MainDispatcherTest() {
    private val repository = FakeTenantRepository()
    private val firstTerm = rentTerm("r1", "2024-01", amountCents = 145_000, dueDay = 1, version = 2)
    private val laterTerm = rentTerm("r2", "2025-01", amountCents = 150_000, dueDay = 5, version = 4)
    private val today = LocalDate(2026, 3, 10)

    private fun TestScope.openEditor(rentTermId: String?): RentTermEditorViewModel {
        val editor = RentTermEditorViewModel(tenantId = "t1", rentTermId = rentTermId, tenantRepository = repository)
        runCurrent()
        return editor
    }

    private fun showTenant() {
        repository.tenant.value = synced(tenantOverview(activeRentTerms = listOf(firstTerm, laterTerm), today = today))
    }

    @Test
    fun aRentChangeStartsNextMonthWithTheDueDayInEffect() =
        runTest {
            showTenant()

            val newTerm = openEditor(rentTermId = null)

            assertEquals(
                RentTermEditorUiState(
                    isNewTerm = true,
                    isLoading = false,
                    startMonth = YearMonth(2026, 4),
                    amount = "",
                    dueDay = "5",
                ),
                newTerm.uiState.value,
            )
        }

    @Test
    fun anExistingTermLoadsIntoTheForm() =
        runTest {
            showTenant()
            val editedLaterTerm = openEditor(rentTermId = "r2")

            assertEquals(
                RentTermEditorUiState(
                    isNewTerm = false,
                    isLoading = false,
                    startMonth = YearMonth(2025, 1),
                    amount = "1,500.00",
                    dueDay = "5",
                    pastMonthWarning = PastMonthWarningUiState.CheckingBalance,
                    canDelete = true,
                ),
                editedLaterTerm.uiState.value,
            )
        }

    @Test
    fun aTermThatNoLongerExistsShowsNotFound() =
        runTest {
            showTenant()
            val editor = openEditor(rentTermId = "gone")

            assertEquals(UiText.Resource(Res.string.tenant_error_not_found), editor.uiState.value.error)
        }

    @Test
    fun aStartMonthInThePastShowsHowTheBalanceChangesBeforeSaving() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            repository.previewResult =
                Ok(RentTermPreview(before = BalanceTotal(55_000, 0), after = BalanceTotal(65_000, 0)))
            newTerm.onFieldChanged(RentTermEditorField.Amount, "1600")
            runCurrent()

            newTerm.onStartMonthChanged(YearMonth(2026, 2))

            assertEquals(PastMonthWarningUiState.CheckingBalance, newTerm.uiState.value.pastMonthWarning)
            advanceUntilIdle()
            assertEquals(
                PastMonthWarningUiState.BalanceChange(
                    before = UiText.Resource(Res.string.balance_owes, listOf("$550.00")),
                    after = UiText.Resource(Res.string.balance_owes, listOf("$650.00")),
                ),
                newTerm.uiState.value.pastMonthWarning,
            )
            val draft = RentTermDraft(startMonth = YearMonth(2026, 2), amountCents = 160_000, dueDay = 5)
            assertEquals(
                listOf<TenantRepositoryCall>(
                    TenantRepositoryCall.PreviewRentTerm("t1", rentTermId = null, draft = draft),
                ),
                repository.calls,
            )
        }

    @Test
    fun editingAPastTermPreviewsThatTermsChange() =
        runTest {
            showTenant()
            val editedLaterTerm = openEditor(rentTermId = "r2")
            repository.previewResult =
                Ok(RentTermPreview(before = BalanceTotal(0, 0), after = BalanceTotal(10_000, 0)))
            runCurrent()

            editedLaterTerm.onFieldChanged(RentTermEditorField.Amount, "1510")
            advanceUntilIdle()

            assertEquals(
                PastMonthWarningUiState.BalanceChange(
                    before = UiText.Resource(Res.string.balance_paid_up),
                    after = UiText.Resource(Res.string.balance_owes, listOf("$100.00")),
                ),
                editedLaterTerm.uiState.value.pastMonthWarning,
            )
            val previewed = repository.calls.last() as TenantRepositoryCall.PreviewRentTerm
            assertEquals("r2", previewed.rentTermId)
            assertEquals(151_000, previewed.draft.amountCents)
        }

    @Test
    fun theCurrentAndFutureMonthsNeverAskForAPreview() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            newTerm.onFieldChanged(RentTermEditorField.Amount, "1600")

            listOf(YearMonth(2026, 3), YearMonth(2026, 4), YearMonth(2027, 1)).forEach { month ->
                newTerm.onStartMonthChanged(month)
                advanceUntilIdle()

                assertNull(newTerm.uiState.value.pastMonthWarning, "month = $month")
            }
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun movingBackToTheCurrentMonthClearsThePastMonthWarning() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            newTerm.onFieldChanged(RentTermEditorField.Amount, "1600")
            newTerm.onStartMonthChanged(YearMonth(2026, 2))
            advanceUntilIdle()

            newTerm.onStartMonthChanged(YearMonth(2026, 3))
            advanceUntilIdle()

            assertNull(newTerm.uiState.value.pastMonthWarning)
        }

    @Test
    fun aFailedPreviewStillWarnsAndSaysWhy() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            repository.previewResult = Err(TenantError.Offline)
            newTerm.onFieldChanged(RentTermEditorField.Amount, "1600")

            newTerm.onStartMonthChanged(YearMonth(2026, 2))
            advanceUntilIdle()

            assertEquals(
                PastMonthWarningUiState.CheckFailed(UiText.Resource(Res.string.tenant_error_offline)),
                newTerm.uiState.value.pastMonthWarning,
            )
        }

    @Test
    fun changingTheDueDayRemindsThatTheTransitionMayNeedACustomDebt() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            assertEquals(false, newTerm.uiState.value.showsDueDayReminder)

            newTerm.onFieldChanged(RentTermEditorField.DueDay, "15")
            assertEquals(true, newTerm.uiState.value.showsDueDayReminder)

            newTerm.onFieldChanged(RentTermEditorField.DueDay, "5")
            assertEquals(false, newTerm.uiState.value.showsDueDayReminder)
        }

    @Test
    fun theAmountAndDueDayAreCheckedForFormatBeforeSaving() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            newTerm.onFieldChanged(RentTermEditorField.DueDay, "fifth")

            newTerm.onSaveClicked()
            runCurrent()

            assertEquals(
                mapOf(
                    RentTermEditorField.Amount to UiText.Resource(Res.string.input_error_amount_format),
                    RentTermEditorField.DueDay to UiText.Resource(Res.string.tenant_editor_error_due_day),
                ),
                newTerm.uiState.value.fieldErrors,
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun savingARentChangeAddsTheTermAndCloses() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)

            newTerm.events.test {
                runCurrent()
                newTerm.onFieldChanged(RentTermEditorField.Amount, "1,600")
                newTerm.onFieldChanged(RentTermEditorField.DueDay, "31")
                newTerm.onSaveClicked()

                assertEquals(RentTermEditorEvent.RentTermSaved, awaitItem())
            }

            val draft = RentTermDraft(startMonth = YearMonth(2026, 4), amountCents = 160_000, dueDay = 31)
            assertEquals(listOf<TenantRepositoryCall>(TenantRepositoryCall.AddRentTerm("t1", draft)), repository.calls)
        }

    @Test
    fun aDueDayOutsideOneToThirtyOneIsFlaggedAsItIsTyped() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            runCurrent()

            newTerm.onFieldChanged(RentTermEditorField.DueDay, "32")

            assertEquals(
                mapOf(RentTermEditorField.DueDay to UiText.Resource(Res.string.tenant_editor_error_due_day)),
                newTerm.uiState.value.fieldErrors,
            )
        }

    @Test
    fun aServerRejectionOfTheStartMonthIsShownUnderTheStartMonth() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            repository.addRentTermResult =
                Err(TenantError.ValidationFailed("A rent term already starts in this month.", field = "Start month"))
            newTerm.onFieldChanged(RentTermEditorField.Amount, "1600")

            newTerm.onSaveClicked()
            runCurrent()

            assertEquals(
                mapOf(RentTermEditorField.StartMonth to UiText.Plain("A rent term already starts in this month.")),
                newTerm.uiState.value.fieldErrors,
            )
            assertEquals(null, newTerm.uiState.value.error)
            assertEquals(false, newTerm.uiState.value.isSaving)

            newTerm.onStartMonthChanged(YearMonth(2026, 5))

            assertEquals(emptyMap(), newTerm.uiState.value.fieldErrors)
        }

    @Test
    fun aServerRejectionWithoutAFieldIsShownAtTheTop() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            repository.addRentTermResult = Err(TenantError.ValidationFailed("This tenant is archived."))
            newTerm.onFieldChanged(RentTermEditorField.Amount, "1600")

            newTerm.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Plain("This tenant is archived."), newTerm.uiState.value.error)
        }

    @Test
    fun savingAnEditSendsTheVersionTheFormOpenedWith() =
        runTest {
            showTenant()
            val editedLaterTerm = openEditor(rentTermId = "r2")

            editedLaterTerm.events.test {
                runCurrent()
                editedLaterTerm.onStartMonthChanged(YearMonth(2026, 6))
                editedLaterTerm.onSaveClicked()

                assertEquals(RentTermEditorEvent.RentTermSaved, awaitItem())
            }

            val draft = RentTermDraft(startMonth = YearMonth(2026, 6), amountCents = 150_000, dueDay = 5)
            assertEquals(
                listOf<TenantRepositoryCall>(
                    TenantRepositoryCall.EditRentTerm("r2", expectedVersion = 4, draft = draft),
                ),
                repository.calls.filterNot { it is TenantRepositoryCall.PreviewRentTerm },
            )
        }

    @Test
    fun anEditConflictShowsWhoChangedItAndReloadsTheTerm() =
        runTest {
            showTenant()
            val editedLaterTerm = openEditor(rentTermId = "r2")
            repository.changeResult =
                Err(TenantError.EditConflict("Sam changed this rent term while you were editing."))
            editedLaterTerm.onFieldChanged(RentTermEditorField.Amount, "1700")
            val samsTerm = laterTerm.copy(amountCents = 155_000, dueDay = 10, version = 5)
            repository.tenant.value =
                synced(tenantOverview(activeRentTerms = listOf(firstTerm, samsTerm), today = today))

            editedLaterTerm.onSaveClicked()
            runCurrent()

            val state = editedLaterTerm.uiState.value
            assertEquals(UiText.Plain("Sam changed this rent term while you were editing."), state.error)
            assertEquals("1,550.00", state.amount)
            assertEquals("10", state.dueDay)
            assertEquals(false, state.isSaving)
        }

    @Test
    fun theTenantsFirstTermCannotBeDeleted() =
        runTest {
            showTenant()
            val newTerm = openEditor(rentTermId = null)
            val editedLaterTerm = openEditor(rentTermId = "r2")

            val editedFirstTerm = openEditor(rentTermId = "r1")

            assertEquals(false, editedFirstTerm.uiState.value.canDelete)
            assertEquals(true, editedLaterTerm.uiState.value.canDelete)
            assertEquals(false, newTerm.uiState.value.canDelete)
        }

    @Test
    fun deletingAsksForAReasonFirst() =
        runTest {
            showTenant()
            val editedLaterTerm = openEditor(rentTermId = "r2")
            editedLaterTerm.onDeleteClicked()
            assertEquals(RentTermDeletionUiState(), editedLaterTerm.uiState.value.deletion)

            editedLaterTerm.onFieldChanged(RentTermEditorField.DeleteReason, "   ")
            editedLaterTerm.onDeleteConfirmed()
            runCurrent()

            assertEquals(
                RentTermDeletionUiState(
                    reason = "   ",
                    error = UiText.Resource(Res.string.rent_term_editor_error_reason_required),
                ),
                editedLaterTerm.uiState.value.deletion,
            )
            assertTrue(repository.calls.none { it is TenantRepositoryCall.DeleteRentTerm })
        }

    @Test
    fun deletingWithAReasonSendsItWithTheVersionAndCloses() =
        runTest {
            showTenant()
            val editedLaterTerm = openEditor(rentTermId = "r2")

            editedLaterTerm.events.test {
                runCurrent()
                editedLaterTerm.onDeleteClicked()
                editedLaterTerm.onFieldChanged(RentTermEditorField.DeleteReason, " Entered by mistake ")
                editedLaterTerm.onDeleteConfirmed()

                assertEquals(RentTermEditorEvent.RentTermDeleted, awaitItem())
            }

            assertEquals(
                listOf<TenantRepositoryCall>(TenantRepositoryCall.DeleteRentTerm("r2", 4, "Entered by mistake")),
                repository.calls.filterNot { it is TenantRepositoryCall.PreviewRentTerm },
            )
        }

    @Test
    fun dismissingTheDeleteDialogDeletesNothing() =
        runTest {
            showTenant()
            val editedLaterTerm = openEditor(rentTermId = "r2")
            editedLaterTerm.onDeleteClicked()

            editedLaterTerm.onDeleteDismissed()
            editedLaterTerm.onDeleteConfirmed()
            runCurrent()

            assertNull(editedLaterTerm.uiState.value.deletion)
            assertTrue(repository.calls.none { it is TenantRepositoryCall.DeleteRentTerm })
        }

    @Test
    fun aRejectedDeleteShowsTheServersMessageInTheDialog() =
        runTest {
            showTenant()
            val editedLaterTerm = openEditor(rentTermId = "r2")
            repository.changeResult = Err(TenantError.TenantArchived("This tenant is archived."))
            editedLaterTerm.onDeleteClicked()
            editedLaterTerm.onFieldChanged(RentTermEditorField.DeleteReason, "Mistake")

            editedLaterTerm.onDeleteConfirmed()
            runCurrent()

            assertEquals(
                RentTermDeletionUiState(reason = "Mistake", error = UiText.Plain("This tenant is archived.")),
                editedLaterTerm.uiState.value.deletion,
            )
        }
}
