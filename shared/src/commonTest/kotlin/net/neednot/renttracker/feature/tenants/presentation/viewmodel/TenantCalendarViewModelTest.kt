package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.CalendarMonthState
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.designsystem.components.CalendarState
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.AllocationTarget
import net.neednot.renttracker.feature.tenants.domain.CalendarMonth
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.FakeTenantCalendarRepository
import net.neednot.renttracker.feature.tenants.domain.FakeTenantRepository
import net.neednot.renttracker.feature.tenants.domain.MonthLateFee
import net.neednot.renttracker.feature.tenants.domain.MonthPayment
import net.neednot.renttracker.feature.tenants.domain.OtherAllocation
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.domain.calendarMonth
import net.neednot.renttracker.feature.tenants.domain.monthDetail
import net.neednot.renttracker.feature.tenants.domain.monthPayment
import net.neednot.renttracker.feature.tenants.domain.synced
import net.neednot.renttracker.feature.tenants.domain.syncedCalendar
import net.neednot.renttracker.feature.tenants.domain.tenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.AddFeeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_description_fee_unpaid
import net.neednot.renttracker.resources.calendar_fee_added_named
import net.neednot.renttracker.resources.calendar_fee_marker
import net.neednot.renttracker.resources.calendar_fee_rent_first
import net.neednot.renttracker.resources.calendar_fee_title_unpaid
import net.neednot.renttracker.resources.calendar_late_paid_some
import net.neednot.renttracker.resources.calendar_other_finished
import net.neednot.renttracker.resources.calendar_other_many
import net.neednot.renttracker.resources.calendar_paid_after_grace
import net.neednot.renttracker.resources.calendar_paid_note
import net.neednot.renttracker.resources.calendar_prepaid_note
import net.neednot.renttracker.resources.calendar_square_owed
import net.neednot.renttracker.resources.calendar_state_partial
import net.neednot.renttracker.resources.calendar_total_owed
import net.neednot.renttracker.resources.calendar_total_owed_on_rent
import net.neednot.renttracker.resources.ledger_may_not_apply
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TenantCalendarViewModelTest : MainDispatcherTest() {
    private val tenants = FakeTenantRepository()
    private val repository = FakeTenantCalendarRepository()
    private val viewModel by lazy {
        TenantViewModel(
            tenantId = "t1",
            tenantRepository = tenants,
            calendarRepository = repository,
            timeZone = TimeZone.UTC,
        )
    }

    @Test
    fun theCalendarStartsOnTheCurrentYearWithTwelveSquares() =
        runTest {
            repository.calendar.value = syncedCalendar(2026, calendarMonth("2026-01"))

            val calendar = assertNotNull(contentOf().calendar)

            assertEquals("2026", calendar.year)
            assertEquals(12, calendar.squares.size)
            assertEquals(listOf(0), repository.requestedYearOffsets)
        }

    @Test
    fun theArrowsMoveTheCalendarOneYearAtATime() =
        runTest {
            contentOf()

            viewModel.onPreviousYearClicked()
            runCurrent()
            viewModel.onPreviousYearClicked()
            runCurrent()
            viewModel.onNextYearClicked()
            runCurrent()

            assertEquals(listOf(0, -1, -2, -1), repository.requestedYearOffsets)
        }

    @Test
    fun monthsMissingFromTheYearAreBlankAndNotTappable() =
        runTest {
            repository.calendar.value = syncedCalendar(2026, calendarMonth("2026-02"))

            val squares = assertNotNull(contentOf().calendar).squares

            assertEquals(CalendarState.NotATenant, squares.first().state)
            assertEquals(false, squares.first().isTappable)
            assertEquals(true, squares[1].isTappable)
        }

    @Test
    fun aPartialMonthIsAnnouncedWithItsStateAndWhatIsOwed() =
        runTest {
            repository.calendar.value =
                syncedCalendar(
                    2026,
                    calendarMonth("2026-03", CalendarMonthState.Partial, coveredCents = 100_000, owedCents = 50_000),
                )

            val square = assertNotNull(contentOf().calendar).squares[2]

            assertEquals(
                listOf(
                    UiText.Plain("March"),
                    UiText.Resource(Res.string.calendar_state_partial),
                    UiText.Resource(Res.string.calendar_square_owed, listOf("$500.00")),
                ),
                square.description,
            )
            assertEquals(UiText.Resource(Res.string.calendar_square_owed, listOf("$500.00")), square.owed)
        }

    @Test
    fun anUnpaidLateFeeAddsAMarkerWithMoneyAndAnAnnouncement() =
        runTest {
            repository.calendar.value =
                syncedCalendar(2026, calendarMonth("2026-02", hasUnpaidLateFee = true, lateFeeUnpaidCents = 5_000))

            val square = assertNotNull(contentOf().calendar).squares[1]

            assertEquals(
                UiText.Resource(Res.string.calendar_fee_marker, listOf("$50.00")),
                square.feeMarker,
            )
            assertEquals(
                UiText.Resource(Res.string.calendar_description_fee_unpaid, listOf("$50.00")),
                square.description.last(),
            )
        }

    @Test
    fun tappingAMonthOpensItsDetail() =
        runTest {
            repository.monthDetail.value = monthDetail(calendarMonth("2026-03"))
            contentOf()

            viewModel.onMonthClicked("2026-03")
            runCurrent()

            val detail = assertNotNull(currentContent().monthDetail)
            assertEquals("March 2026", detail.title)
            assertEquals(listOf(YearMonth(2026, 3)), repository.requestedMonths)
        }

    @Test
    fun dismissingTheDetailClosesIt() =
        runTest {
            repository.monthDetail.value = monthDetail()
            contentOf()
            viewModel.onMonthClicked("2026-03")
            runCurrent()

            viewModel.onMonthDetailDismissed()
            runCurrent()

            assertNull(currentContent().monthDetail)
        }

    @Test
    fun aLateMonthWithoutAFeeOffersAddFeeWhenTheCallerMayAddDebts() =
        runTest {
            openDetail(lateMonthWithoutFee(), canAddCharge = true)

            val notice = assertNotNull(currentContent().monthDetail?.lateNotice)

            assertEquals(AddFeeUiState("2026-03", 5_000), notice.addFee)
            assertEquals(
                UiText.Resource(Res.string.calendar_late_paid_some, listOf("$1,000.00", "Mar 6")),
                notice.message,
            )
        }

    @Test
    fun aLateMonthWithoutAFeeHasNoAddFeeButtonWhenTheCallerMayNotAddDebts() =
        runTest {
            openDetail(lateMonthWithoutFee(), canAddCharge = false)

            val notice = assertNotNull(currentContent().monthDetail?.lateNotice)

            assertNull(notice.addFee)
        }

    @Test
    fun aMonthThatWasNotLateHasNoLateNotice() =
        runTest {
            openDetail(calendarMonth("2026-03", wasLate = false), canAddCharge = true)

            assertNull(currentContent().monthDetail?.lateNotice)
        }

    @Test
    fun aSinglePaymentSplitUsesTheOtherAmountAndSaysItFinishedTheEarlierMonth() =
        runTest {
            val payment =
                monthPayment(
                    otherAllocations = listOf(OtherAllocation(AllocationTarget.Rent(YearMonth(2026, 2)), 50_000)),
                )
            openDetail(calendarMonth("2026-03", CalendarMonthState.Partial), payments = listOf(payment))

            val row = currentContent().monthDetail?.payments?.single()

            assertEquals(
                listOf(UiText.Resource(Res.string.calendar_other_finished, listOf("$500.00", "$1,500.00", "February"))),
                row?.notes,
            )
            assertEquals("$1,000.00", row?.amount)
        }

    @Test
    fun severalOtherAllocationsAreEachListed() =
        runTest {
            val payment =
                monthPayment(
                    otherAllocations =
                        listOf(
                            OtherAllocation(AllocationTarget.Rent(YearMonth(2026, 1)), 30_000),
                            OtherAllocation(AllocationTarget.Charge(ChargeKind.Custom("Water")), 2_000),
                        ),
                )
            openDetail(calendarMonth("2026-03"), payments = listOf(payment))

            assertEquals(
                listOf(
                    UiText.Resource(Res.string.calendar_other_many, listOf("$300.00", "January")),
                    UiText.Resource(Res.string.calendar_other_many, listOf("$20.00", "Water")),
                ),
                currentContent()
                    .monthDetail
                    ?.payments
                    ?.single()
                    ?.notes,
            )
        }

    @Test
    fun aPaymentAfterTheGracePeriodOfALateMonthSaysSo() =
        runTest {
            val month = calendarMonth("2026-02", wasLate = true, graceEnd = LocalDate(2026, 2, 6))
            openDetail(month, payments = listOf(monthPayment(dateReceived = LocalDate(2026, 3, 2))))

            assertEquals(
                listOf(UiText.Resource(Res.string.calendar_paid_after_grace, listOf("Feb 6", "February"))),
                currentContent()
                    .monthDetail
                    ?.payments
                    ?.single()
                    ?.notes,
            )
        }

    @Test
    fun aPaymentWithinTheGracePeriodOrOnAMonthThatWasNotLateHasNoGraceNote() =
        runTest {
            val lateMonth = calendarMonth("2026-02", wasLate = true, graceEnd = LocalDate(2026, 2, 6))
            openDetail(lateMonth, payments = listOf(monthPayment(dateReceived = LocalDate(2026, 2, 6))))
            assertEquals(
                emptyList(),
                currentContent()
                    .monthDetail
                    ?.payments
                    ?.single()
                    ?.notes,
            )

            val onTimeMonth = calendarMonth("2026-02", wasLate = false, graceEnd = LocalDate(2026, 2, 6))
            repository.monthDetail.value =
                monthDetail(onTimeMonth, payments = listOf(monthPayment(dateReceived = LocalDate(2026, 3, 2))))
            runCurrent()
            assertEquals(
                emptyList(),
                currentContent()
                    .monthDetail
                    ?.payments
                    ?.single()
                    ?.notes,
            )
        }

    @Test
    fun anUnpaidFeeUnderRentFirstExplainsThatPaymentsWentToRentAndRelabelsWhatIsStillOwed() =
        runTest {
            val fee =
                MonthLateFee(
                    5_000,
                    isUnpaid = true,
                    addedOn = LocalDate(2026, 2, 6),
                    addedBy = Person.Named("Sam Rivera"),
                    mayNotApply = false,
                )
            openDetail(calendarMonth("2026-02", hasUnpaidLateFee = true), lateFee = fee)

            val detail = assertNotNull(currentContent().monthDetail)

            assertEquals(
                UiText.Resource(Res.string.calendar_fee_title_unpaid, listOf("February")),
                detail.lateFee?.title,
            )
            assertEquals(
                listOf(
                    UiText.Resource(Res.string.calendar_fee_added_named, listOf("Feb 6", "Sam Rivera")),
                    UiText.Resource(Res.string.calendar_fee_rent_first),
                ),
                detail.lateFee?.notes,
            )
            assertEquals(UiText.Resource(Res.string.calendar_total_owed_on_rent), detail.totals.last().label)
            assertNull(detail.lateNotice)
        }

    @Test
    fun aFeeThatMayNotApplyCarriesThatLabel() =
        runTest {
            val fee =
                MonthLateFee(
                    5_000,
                    isUnpaid = false,
                    addedOn = LocalDate(2026, 2, 6),
                    addedBy = Person.You,
                    mayNotApply = true,
                )
            openDetail(calendarMonth("2026-02"), lateFee = fee, paymentOrder = PaymentOrder.ByDate)

            assertEquals(
                UiText.Resource(Res.string.ledger_may_not_apply),
                currentContent()
                    .monthDetail
                    ?.lateFee
                    ?.notes
                    ?.last(),
            )
        }

    @Test
    fun withoutAFeeStillOwedIsPlain() =
        runTest {
            openDetail(calendarMonth("2026-03", CalendarMonthState.Partial, coveredCents = 100_000, owedCents = 50_000))

            assertEquals(
                UiText.Resource(Res.string.calendar_total_owed),
                currentContent()
                    .monthDetail
                    ?.totals
                    ?.last()
                    ?.label,
            )
        }

    @Test
    fun anUpcomingMonthPartlyPrepaidShowsThePrepaidAmount() =
        runTest {
            openDetail(calendarMonth("2026-04", CalendarMonthState.Upcoming, coveredCents = 50_000))

            val detail = assertNotNull(currentContent().monthDetail)

            assertEquals(UiText.Resource(Res.string.calendar_prepaid_note, listOf("$500.00")), detail.coverageNote)
            assertTrue(detail.totals.isEmpty())
        }

    @Test
    fun aMonthDueButNotLateShowsWhatWasPaid() =
        runTest {
            openDetail(calendarMonth("2026-03", CalendarMonthState.DueNotLate, coveredCents = 70_000))

            assertEquals(
                UiText.Resource(Res.string.calendar_paid_note, listOf("$700.00")),
                currentContent().monthDetail?.coverageNote,
            )
        }

    private fun lateMonthWithoutFee() =
        calendarMonth(
            "2026-03",
            CalendarMonthState.Partial,
            wasLate = true,
            graceEnd = LocalDate(2026, 3, 6),
            paidByGraceEndCents = 100_000,
            suggestedLateFeeCents = 5_000,
        )

    private suspend fun TestScope.openDetail(
        month: CalendarMonth,
        canAddCharge: Boolean = true,
        payments: List<MonthPayment> = emptyList(),
        lateFee: MonthLateFee? = null,
        paymentOrder: PaymentOrder = PaymentOrder.RentFirst,
    ) {
        repository.monthDetail.value = monthDetail(month, payments, lateFee, paymentOrder, canAddCharge)
        contentOf()
        viewModel.onMonthClicked(month.month.toString())
        runCurrent()
    }

    private suspend fun TestScope.contentOf(): TenantUiState.Content {
        tenants.tenant.value = synced(tenantOverview())
        runCurrent()
        return currentContent()
    }

    private suspend fun currentContent(): TenantUiState.Content =
        viewModel.uiState.filterIsInstance<TenantUiState.Content>().first()
}
