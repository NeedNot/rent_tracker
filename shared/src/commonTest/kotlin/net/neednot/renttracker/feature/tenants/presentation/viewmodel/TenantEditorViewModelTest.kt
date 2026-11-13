package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.FakeTenantRepository
import net.neednot.renttracker.feature.tenants.domain.FirstRentTerm
import net.neednot.renttracker.feature.tenants.domain.TenantDetails
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantRepositoryCall
import net.neednot.renttracker.feature.tenants.domain.synced
import net.neednot.renttracker.feature.tenants.domain.tenantDetails
import net.neednot.renttracker.feature.tenants.domain.tenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorForm
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_editor_error_amount_format
import net.neednot.renttracker.resources.tenant_editor_error_amount_positive
import net.neednot.renttracker.resources.tenant_editor_error_due_day_format
import net.neednot.renttracker.resources.tenant_editor_error_move_in_required
import net.neednot.renttracker.resources.tenant_editor_error_name_required
import net.neednot.renttracker.resources.tenant_editor_error_whole_days
import net.neednot.renttracker.resources.tenant_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TenantEditorViewModelTest : MainDispatcherTest() {
    private val repository = FakeTenantRepository()
    private val newTenant by lazy {
        TenantEditorViewModel(
            groupId = "g1",
            tenantId = null,
            tenantRepository = repository,
        )
    }
    private val savedTenant by lazy {
        TenantEditorViewModel(groupId = "g1", tenantId = "t1", tenantRepository = repository)
    }

    @Test
    fun aNewTenantStartsWithAnEmptyForm() =
        runTest {
            assertEquals(TenantEditorUiState(isNewTenant = true), newTenant.uiState.value)
        }

    @Test
    fun requiredFieldsAreCheckedBeforeSaving() =
        runTest {
            newTenant.onSaveClicked()
            runCurrent()

            assertEquals(
                mapOf(
                    TenantEditorField.Name to UiText.Resource(Res.string.tenant_editor_error_name_required),
                    TenantEditorField.MoveIn to UiText.Resource(Res.string.tenant_editor_error_move_in_required),
                    TenantEditorField.MonthlyRent to UiText.Resource(Res.string.tenant_editor_error_amount_format),
                    TenantEditorField.DueDay to UiText.Resource(Res.string.tenant_editor_error_due_day_format),
                ),
                newTenant.uiState.value.fieldErrors,
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun amountsAndNumbersAreCheckedForFormatBeforeSaving() =
        runTest {
            newTenant.fillRequiredFields()
            newTenant.onFieldChanged(TenantEditorField.MonthlyRent, "0")
            newTenant.onFieldChanged(TenantEditorField.SecurityDeposit, "1.234")
            newTenant.onFieldChanged(TenantEditorField.LateFee, "fifty")
            newTenant.onFieldChanged(TenantEditorField.GraceDays, "2.5")
            newTenant.onFieldChanged(TenantEditorField.DueDay, "first")

            newTenant.onSaveClicked()
            runCurrent()

            assertEquals(
                mapOf(
                    TenantEditorField.MonthlyRent to UiText.Resource(Res.string.tenant_editor_error_amount_positive),
                    TenantEditorField.SecurityDeposit to UiText.Resource(Res.string.tenant_editor_error_amount_format),
                    TenantEditorField.LateFee to UiText.Resource(Res.string.tenant_editor_error_amount_format),
                    TenantEditorField.GraceDays to UiText.Resource(Res.string.tenant_editor_error_whole_days),
                    TenantEditorField.DueDay to UiText.Resource(Res.string.tenant_editor_error_due_day_format),
                ),
                newTenant.uiState.value.fieldErrors,
            )
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun editingAFieldClearsItsError() =
        runTest {
            newTenant.onSaveClicked()

            newTenant.onFieldChanged(TenantEditorField.Name, "Apt 3B")
            newTenant.onMoveInChanged(LocalDate(2026, 1, 17))

            assertEquals(
                setOf(TenantEditorField.MonthlyRent, TenantEditorField.DueDay),
                newTenant.uiState.value.fieldErrors.keys,
            )
        }

    @Test
    fun rulesBeyondFormatAreLeftToTheServerAndItsMessageIsShown() =
        runTest {
            repository.addTenantResult = Err(TenantError.ValidationFailed("Due day must be between 1 and 31."))
            newTenant.fillRequiredFields()
            newTenant.onFieldChanged(TenantEditorField.DueDay, "45")
            newTenant.onFieldChanged(TenantEditorField.Phone, "not a phone")

            newTenant.onSaveClicked()
            runCurrent()

            assertEquals(1, repository.calls.size)
            assertEquals(UiText.Plain("Due day must be between 1 and 31."), newTenant.uiState.value.error)
            assertEquals(false, newTenant.uiState.value.isSaving)
        }

    @Test
    fun addingSendsTheTrimmedDetailsWithTheFirstRentTermAndOpensTheNewTenant() =
        runTest {
            repository.addTenantResult = Ok("t7")

            newTenant.events.test {
                newTenant.onFieldChanged(TenantEditorField.Name, "  Apt 3B ")
                newTenant.onFieldChanged(TenantEditorField.ContactName, " Dana Whitfield")
                newTenant.onFieldChanged(TenantEditorField.Email, "   ")
                newTenant.onFieldChanged(TenantEditorField.SecurityDeposit, "1,500")
                newTenant.onFieldChanged(TenantEditorField.GraceDays, "3")
                newTenant.onFieldChanged(TenantEditorField.LateFee, "$75.5")
                newTenant.onFieldChanged(TenantEditorField.MonthlyRent, "1,234.56")
                newTenant.onFieldChanged(TenantEditorField.DueDay, "15")
                newTenant.onMoveInChanged(LocalDate(2026, 1, 17))
                newTenant.onLastDayInUnitChanged(LocalDate(2026, 12, 31))
                newTenant.onSaveClicked()

                assertEquals(TenantEditorEvent.TenantAdded("t7"), awaitItem())
            }

            val expectedDetails =
                TenantDetails(
                    name = "Apt 3B",
                    contactName = "Dana Whitfield",
                    contactPhone = null,
                    contactEmail = null,
                    notes = null,
                    depositCents = 150_000,
                    moveIn = LocalDate(2026, 1, 17),
                    moveOut = LocalDate(2026, 12, 31),
                    graceDaysOverride = 3,
                    lateFeeFlatOverrideCents = 7_550,
                )
            assertEquals(
                listOf<TenantRepositoryCall>(
                    TenantRepositoryCall.AddTenant(
                        "g1",
                        expectedDetails,
                        FirstRentTerm(amountCents = 123_456, dueDay = 15),
                    ),
                ),
                repository.calls,
            )
        }

    @Test
    fun beingOfflineShowsTheOfflineMessageAndKeepsTheForm() =
        runTest {
            repository.addTenantResult = Err(TenantError.Offline)
            newTenant.fillRequiredFields()

            newTenant.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Resource(Res.string.tenant_error_offline), newTenant.uiState.value.error)
            assertEquals("Apt 3B", newTenant.uiState.value.form.name)
        }

    @Test
    fun anExistingTenantLoadsIntoTheForm() =
        runTest {
            val details = tenantDetails(moveOut = LocalDate(2026, 3, 31), graceDaysOverride = 2)
            repository.tenant.value = Synced(tenantOverview(details = details), SyncStatus.Syncing)

            assertEquals(true, savedTenant.uiState.value.isLoading)
            runCurrent()

            val expected =
                TenantEditorForm(
                    name = "Apt 3B",
                    contactName = "Dana Whitfield",
                    phone = "(555) 014-2290",
                    email = "dana.w@example.com",
                    notes = "Two roommates on the lease.",
                    securityDeposit = "1,500.00",
                    moveIn = LocalDate(2024, 1, 1),
                    lastDayInUnit = LocalDate(2026, 3, 31),
                    graceDays = "2",
                    lateFee = "",
                )
            assertEquals(TenantEditorUiState(isNewTenant = false, form = expected), savedTenant.uiState.value)
        }

    @Test
    fun aTenantThatCannotBeLoadedShowsTheError() =
        runTest {
            repository.tenant.value = Synced(null, SyncStatus.Failed(TenantError.Offline))

            savedTenant.uiState.test {
                runCurrent()

                assertEquals(
                    TenantEditorUiState(isNewTenant = false, error = UiText.Resource(Res.string.tenant_error_offline)),
                    expectMostRecentItem(),
                )
            }
        }

    @Test
    fun savingAnEditSendsTheVersionTheFormOpenedWithAndCloses() =
        runTest {
            repository.tenant.value = synced(tenantOverview(version = 5))

            savedTenant.events.test {
                runCurrent()
                savedTenant.onFieldChanged(TenantEditorField.Name, "Apt 3C")
                savedTenant.onLastDayInUnitChanged(null)
                savedTenant.onSaveClicked()

                assertEquals(TenantEditorEvent.TenantSaved, awaitItem())
            }

            assertEquals(
                listOf<TenantRepositoryCall>(
                    TenantRepositoryCall.EditTenant(
                        "t1",
                        expectedVersion = 5,
                        details = tenantDetails(name = "Apt 3C"),
                    ),
                ),
                repository.calls,
            )
        }

    @Test
    fun anEditConflictShowsWhoChangedItAndReloadsTheFormWithTheirValues() =
        runTest {
            repository.tenant.value = synced(tenantOverview(version = 5))
            repository.changeResult = Err(TenantError.EditConflict("Sam changed this tenant while you were editing."))
            savedTenant.uiState.test {
                runCurrent()
                savedTenant.onFieldChanged(TenantEditorField.Name, "Apt 3C")
                repository.tenant.value = synced(tenantOverview(details = tenantDetails(name = "Unit 3B"), version = 6))

                savedTenant.onSaveClicked()
                runCurrent()

                val state = expectMostRecentItem()
                assertEquals(UiText.Plain("Sam changed this tenant while you were editing."), state.error)
                assertEquals("Unit 3B", state.form.name)
                assertEquals(false, state.isSaving)
            }

            repository.changeResult = Ok(Unit)
            savedTenant.onSaveClicked()
            runCurrent()

            assertEquals(
                listOf(5L, 6L),
                repository.calls.map { (it as TenantRepositoryCall.EditTenant).expectedVersion },
            )
        }

    private fun TenantEditorViewModel.fillRequiredFields() {
        onFieldChanged(TenantEditorField.Name, "Apt 3B")
        onFieldChanged(TenantEditorField.MonthlyRent, "1500")
        onFieldChanged(TenantEditorField.DueDay, "1")
        onMoveInChanged(LocalDate(2026, 1, 1))
    }
}
