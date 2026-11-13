package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.FakeTenantRepository
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantGroup
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.TenantRepositoryCall
import net.neednot.renttracker.feature.tenants.domain.rentTerm
import net.neednot.renttracker.feature.tenants.domain.synced
import net.neednot.renttracker.feature.tenants.domain.tenantBalance
import net.neednot.renttracker.feature.tenants.domain.tenantDetails
import net.neednot.renttracker.feature.tenants.domain.tenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantBalanceUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantConfirmationUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantDetailsUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusChangeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_credit
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.tenant_archive_message
import net.neednot.renttracker.resources.tenant_archive_owes
import net.neednot.renttracker.resources.tenant_error_not_found
import net.neednot.renttracker.resources.tenant_error_offline
import net.neednot.renttracker.resources.tenant_grace_days
import net.neednot.renttracker.resources.tenant_grace_days_group
import net.neednot.renttracker.resources.tenant_late_fee_group
import net.neednot.renttracker.resources.tenant_late_fee_percent_of_rent
import net.neednot.renttracker.resources.tenant_next_due
import net.neednot.renttracker.resources.tenant_partial_move_out_warning
import net.neednot.renttracker.resources.tenant_rent_term_from
import net.neednot.renttracker.resources.tenant_rent_term_range
import net.neednot.renttracker.resources.tenant_rent_term_summary
import net.neednot.renttracker.resources.tenant_restore_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TenantViewModelTest : MainDispatcherTest() {
    private val repository = FakeTenantRepository()
    private val viewModel by lazy { TenantViewModel(tenantId = "t1", tenantRepository = repository) }

    @Test
    fun showsLoadingUntilTheTenantArrives() =
        runTest {
            viewModel.uiState.test {
                assertEquals(TenantUiState.Loading, awaitItem())
            }
        }

    @Test
    fun showsTheTenantWithItsGroupBalanceStateAndNextDueDate() =
        runTest {
            val content =
                contentOf(tenantOverview(balance = tenantBalance(TenantBalanceState.Late, owedCents = 55_000)))

            assertEquals("Apt 3B", content.name)
            assertEquals("g1", content.groupId)
            assertEquals("1st Street", content.groupName)
            assertEquals(false, content.isArchived)
            assertEquals(
                TenantBalanceUiState(
                    state = TenantBalanceState.Late,
                    total = UiText.Resource(Res.string.balance_owes, listOf("$550.00")),
                    nextDue = UiText.Resource(Res.string.tenant_next_due, listOf("Apr 1, 2026")),
                    partialMoveOutWarning = null,
                ),
                content.balance,
            )
        }

    @Test
    fun theBalanceTotalIsAlwaysInWords() =
        runTest {
            val credit = contentOf(tenantOverview(balance = tenantBalance(creditCents = 40_000)))
            assertEquals(UiText.Resource(Res.string.balance_credit, listOf("$400.00")), credit.balance?.total)

            val paidUp = contentOf(tenantOverview(balance = tenantBalance(nextDueDate = null)))
            assertEquals(UiText.Resource(Res.string.balance_paid_up), paidUp.balance?.total)
            assertNull(paidUp.balance?.nextDue)
        }

    @Test
    fun aPartialMoveOutMonthWarnsThatItIsNotCharged() =
        runTest {
            val wholeMonth = LocalDate(2026, 3, 1)..LocalDate(2026, 3, 31)
            val acrossMonths = LocalDate(2026, 3, 15)..LocalDate(2026, 4, 14)

            val cases = listOf(wholeMonth to "Mar 1–31", acrossMonths to "Mar 15 – Apr 14")

            cases.forEach { (period, expected) ->
                val content = contentOf(tenantOverview(balance = tenantBalance(partialMoveOutPeriod = period)))
                assertEquals(
                    UiText.Resource(Res.string.tenant_partial_move_out_warning, listOf(expected)),
                    content.balance?.partialMoveOutWarning,
                )
            }
        }

    @Test
    fun rentTermsAreListedNewestFirstWithTheTermInEffectMarkedCurrent() =
        runTest {
            val terms =
                listOf(
                    rentTerm("r1", "2024-01", amountCents = 145_000),
                    rentTerm("r2", "2025-01", amountCents = 150_000, dueDay = 22),
                    rentTerm("r3", "2026-07", amountCents = 160_000, dueDay = 3),
                )

            val content = contentOf(tenantOverview(activeRentTerms = terms, today = LocalDate(2026, 3, 10)))

            val expected =
                listOf(
                    RentTermRowUiState(
                        rentTermId = "r3",
                        summary = UiText.Resource(Res.string.tenant_rent_term_summary, listOf("$1,600.00", "3rd")),
                        period = UiText.Resource(Res.string.tenant_rent_term_from, listOf("Jul 2026")),
                        isCurrent = false,
                    ),
                    RentTermRowUiState(
                        rentTermId = "r2",
                        summary = UiText.Resource(Res.string.tenant_rent_term_summary, listOf("$1,500.00", "22nd")),
                        period = UiText.Resource(Res.string.tenant_rent_term_range, listOf("Jan 2025", "Jun 2026")),
                        isCurrent = true,
                    ),
                    RentTermRowUiState(
                        rentTermId = "r1",
                        summary = UiText.Resource(Res.string.tenant_rent_term_summary, listOf("$1,450.00", "1st")),
                        period = UiText.Resource(Res.string.tenant_rent_term_range, listOf("Jan 2024", "Dec 2024")),
                        isCurrent = false,
                    ),
                )
            assertEquals(expected, content.rentTerms)
        }

    @Test
    fun dueDaysReadAsOrdinals() {
        val cases = listOf(1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th", 11 to "11th", 12 to "12th", 13 to "13th")
        val lateCases = listOf(21 to "21st", 22 to "22nd", 23 to "23rd", 30 to "30th", 31 to "31st")
        (cases + lateCases).forEach { (day, expected) -> assertEquals(expected, ordinal(day)) }
    }

    @Test
    fun detailsShowTheGroupsGraceAndFivePercentLateFeeWhenNothingIsSet() =
        runTest {
            val content = contentOf(tenantOverview())

            assertEquals(
                TenantDetailsUiState(
                    contactName = "Dana Whitfield",
                    phone = "(555) 014-2290",
                    email = "dana.w@example.com",
                    movedIn = "Jan 1, 2024",
                    lastDayInUnit = null,
                    securityDeposit = "$1,500.00",
                    gracePeriod = UiText.Plural(Res.plurals.tenant_grace_days_group, 5, listOf(5)),
                    lateFee = UiText.Resource(Res.string.tenant_late_fee_percent_of_rent),
                    notes = "Two roommates on the lease.",
                ),
                content.details,
            )
        }

    @Test
    fun detailsShowTheGroupsFlatLateFee() =
        runTest {
            val group = TenantGroup(name = "1st Street", graceDays = 5, lateFeeFlatCents = 5_000)

            val content = contentOf(tenantOverview(group = group))

            assertEquals(UiText.Resource(Res.string.tenant_late_fee_group, listOf("$50.00")), content.details.lateFee)
        }

    @Test
    fun detailsShowTheTenantsOwnGraceAndLateFeeAndLastDay() =
        runTest {
            val details =
                tenantDetails(
                    moveOut = LocalDate(2026, 3, 31),
                    graceDaysOverride = 1,
                    lateFeeFlatOverrideCents = 7_500,
                )

            val content = contentOf(tenantOverview(details = details))

            assertEquals(UiText.Plural(Res.plurals.tenant_grace_days, 1, listOf(1)), content.details.gracePeriod)
            assertEquals(UiText.Plain("$75.00"), content.details.lateFee)
            assertEquals("Mar 31, 2026", content.details.lastDayInUnit)
        }

    @Test
    fun anArchivedTenantIsMarkedArchived() =
        runTest {
            assertTrue(contentOf(tenantOverview(isArchived = true)).isArchived)
        }

    @Test
    fun aFailureWithNothingCachedShowsTheError() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.tenant.value = Synced(null, SyncStatus.Failed(TenantError.Offline))

                assertEquals(TenantUiState.Error(UiText.Resource(Res.string.tenant_error_offline)), awaitItem())
            }
        }

    @Test
    fun aTenantTheServerDoesNotReturnIsNotFound() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.tenant.value = synced(null)

                assertEquals(TenantUiState.Error(UiText.Resource(Res.string.tenant_error_not_found)), awaitItem())
            }
        }

    @Test
    fun aFailureKeepsTheCachedTenantOnScreenWithTheError() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)

                repository.tenant.value = Synced(tenantOverview(), SyncStatus.Failed(TenantError.Offline))

                val content = awaitItem() as TenantUiState.Content
                assertEquals(UiText.Resource(Res.string.tenant_error_offline), content.syncError)
            }
        }

    @Test
    fun archivingAsksFirstAndShowsWhatTheTenantStillOwes() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                repository.tenant.value =
                    synced(tenantOverview(balance = tenantBalance(TenantBalanceState.Late, owedCents = 55_000)))
                skipItems(1)

                viewModel.onArchiveClicked()

                val expected =
                    TenantConfirmationUiState(
                        action = TenantStatusAction.Archive,
                        message = UiText.Resource(Res.string.tenant_archive_owes, listOf("Apt 3B", "$550.00")),
                    )
                assertEquals(expected, (awaitItem() as TenantUiState.Content).statusChange.confirmation)
            }
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun archivingAPaidUpTenantAsksWithoutABalance() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                repository.tenant.value = synced(tenantOverview())
                skipItems(1)

                viewModel.onArchiveClicked()

                assertEquals(
                    UiText.Resource(Res.string.tenant_archive_message, listOf("Apt 3B")),
                    (awaitItem() as TenantUiState.Content).statusChange.confirmation?.message,
                )
            }
        }

    @Test
    fun confirmingArchivesWithTheVersionOnScreenAndClosesTheDialog() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                repository.tenant.value = synced(tenantOverview(version = 7))
                skipItems(1)
                viewModel.onArchiveClicked()
                skipItems(1)

                viewModel.onStatusChangeConfirmed()
                runCurrent()

                assertEquals(
                    TenantStatusChangeUiState(),
                    (expectMostRecentItem() as TenantUiState.Content).statusChange,
                )
            }
            assertEquals(listOf<TenantRepositoryCall>(TenantRepositoryCall.ArchiveTenant("t1", 7)), repository.calls)
        }

    @Test
    fun dismissingTheConfirmationArchivesNothing() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                repository.tenant.value = synced(tenantOverview())
                skipItems(1)
                viewModel.onArchiveClicked()
                skipItems(1)

                viewModel.onStatusChangeDismissed()
                viewModel.onStatusChangeConfirmed()
                runCurrent()

                assertNull((expectMostRecentItem() as TenantUiState.Content).statusChange.confirmation)
            }
            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun restoringAsksFirstThenRestoresWithTheVersionOnScreen() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                repository.tenant.value = synced(tenantOverview(isArchived = true, version = 4))
                skipItems(1)

                viewModel.onRestoreClicked()

                val expected =
                    TenantConfirmationUiState(
                        action = TenantStatusAction.Restore,
                        message = UiText.Resource(Res.string.tenant_restore_message, listOf("Apt 3B")),
                    )
                assertEquals(expected, (awaitItem() as TenantUiState.Content).statusChange.confirmation)

                viewModel.onStatusChangeConfirmed()
                runCurrent()
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf<TenantRepositoryCall>(TenantRepositoryCall.RestoreTenant("t1", 4)), repository.calls)
        }

    @Test
    fun anArchiveConflictShowsWhoChangedTheTenant() =
        runTest {
            repository.changeResult = Err(TenantError.EditConflict("Sam changed this tenant while you were editing."))

            viewModel.uiState.test {
                skipItems(1)
                repository.tenant.value = synced(tenantOverview())
                skipItems(1)
                viewModel.onArchiveClicked()
                skipItems(1)

                viewModel.onStatusChangeConfirmed()
                runCurrent()

                assertEquals(
                    TenantStatusChangeUiState(error = UiText.Plain("Sam changed this tenant while you were editing.")),
                    (expectMostRecentItem() as TenantUiState.Content).statusChange,
                )
            }
        }

    private suspend fun TestScope.contentOf(overview: TenantOverview): TenantUiState.Content {
        repository.tenant.value = synced(overview)
        runCurrent()
        return viewModel.uiState.filterIsInstance<TenantUiState.Content>().first()
    }
}
