package net.neednot.renttracker.screenshots

import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.designsystem.components.CalendarState
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.presentation.uistate.BalanceLineUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.CalendarSquareUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.CalendarUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.LateFeeSuggestionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerItemUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantBalanceUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantDetailsUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantUiState

internal const val OFFLINE_TEXT = "Offline · last updated 2h ago"

internal fun ledgerRow(
    date: String,
    title: String,
    amount: String,
    balance: String? = null,
    details: List<String> = emptyList(),
    chips: List<EntryChip> = emptyList(),
    isPaidAmount: Boolean = false,
    isStruckThrough: Boolean = false,
    mayNotApply: Boolean = false,
    canRestore: Boolean = false,
): LedgerItemUiState =
    LedgerItemUiState.Entry(
        LedgerRowUiState(
            key = "$date $title",
            date = date,
            title = UiText.Plain(title),
            details = details.map(UiText::Plain),
            amount = UiText.Plain(amount),
            isPaidAmount = isPaidAmount,
            balance = balance?.let(UiText::Plain),
            chips = chips,
            isStruckThrough = isStruckThrough,
            mayNotApply = mayNotApply,
            canRestore = canRestore,
            isTappable = true,
        ),
    )

internal fun pendingRow(
    chip: PendingChip,
    detail: String,
): LedgerItemUiState =
    LedgerItemUiState.Pending(
        PendingRowUiState(
            clientId = detail,
            date = "Mar 10",
            title = UiText.Plain("Payment · Zelle"),
            chip = chip,
            detail = UiText.Plain(detail),
            amount = UiText.Plain("Paid $550.00"),
        ),
    )

internal val paymentRow =
    ledgerRow(
        "Mar 2",
        "Payment · Zelle",
        "Paid $1,500.00",
        "Owes $550.00",
        listOf("Logged Mar 2 by you"),
        isPaidAmount = true,
    )
internal val rentRow = ledgerRow("Mar 1", "Rent · March", "$1,500.00", "Owes $2,050.00")
internal val lateFeeRow =
    ledgerRow("Feb 6", "Late fee · for February", "$50.00", "Owes $550.00", listOf("Added by Sam Rivera"))

private fun square(
    month: Int,
    name: String,
    state: CalendarState,
    label: String,
    amount: String? = null,
    fee: String? = null,
    isCurrentMonth: Boolean = false,
) = CalendarSquareUiState(
    month = "2026-$month",
    monthName = name,
    state = state,
    stateLabel = UiText.Plain(label),
    amount = amount?.let(UiText::Plain),
    feeMarker = fee?.let(UiText::Plain),
    description = listOf(UiText.Plain(name), UiText.Plain(label)),
    isTappable = state != CalendarState.NotATenant,
    isCurrentMonth = isCurrentMonth,
)

private val laterMonths = listOf("Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private const val FIRST_LATER_MONTH = 4

private fun calendar(firstThree: List<CalendarSquareUiState>) =
    CalendarUiState(
        year = "2026",
        hasPreviousYear = true,
        squares =
            firstThree +
                laterMonths.mapIndexed { index, name ->
                    square(index + FIRST_LATER_MONTH, name, CalendarState.Upcoming, "Upcoming")
                },
    )

private val populatedCalendar =
    calendar(
        listOf(
            square(1, "Jan", CalendarState.Paid, "Paid"),
            square(2, "Feb", CalendarState.Paid, "Paid", fee = "Fee $50.00"),
            square(3, "Mar", CalendarState.Partial, "Partial", amount = "$500.00 owed", isCurrentMonth = true),
        ),
    )

private val emptyCalendar =
    calendar(
        listOf(
            square(1, "Jan", CalendarState.NotATenant, "Not a tenant"),
            square(2, "Feb", CalendarState.NotATenant, "Not a tenant"),
            square(3, "Mar", CalendarState.DueNotLate, "Due", amount = "Due Mar 15", isCurrentMonth = true),
        ),
    )

private fun rentTerm(
    id: String,
    summary: String,
    period: String,
    isCurrent: Boolean,
) = RentTermRowUiState(id, UiText.Plain(summary), UiText.Plain(period), isCurrent)

internal val populatedTenant =
    TenantUiState.Content(
        name = "Apt 3B",
        groupId = "g1",
        groupName = "1st Street",
        isArchived = false,
        balance =
            TenantBalanceUiState(
                state = TenantBalanceState.Late,
                total = UiText.Plain("Owes $550.00"),
                lines =
                    listOf(
                        BalanceLineUiState(UiText.Plain("Rent"), "$500.00"),
                        BalanceLineUiState(UiText.Plain("Late fees"), "$50.00"),
                    ),
                nextDue = null,
                partialMoveOutWarning = null,
            ),
        rentTerms =
            listOf(
                rentTerm("r2", "$1,500.00 a month · due the 1st", "From Jan 2025", isCurrent = true),
                rentTerm("r1", "$1,450.00 a month · due the 1st", "Jan 2024 to Dec 2024", isCurrent = false),
            ),
        details =
            TenantDetailsUiState(
                contactName = "Dana Whitfield",
                phone = "(555) 014-2290",
                email = "dana.w@example.com",
                movedIn = "Jan 1, 2024",
                lastDayInUnit = null,
                securityDeposit = "$1,500.00",
                gracePeriod = UiText.Plain("5 days (group)"),
                lateFee = UiText.Plain("$50.00 (group)"),
                notes = "Two roommates on the lease; Dana sends rent for both.",
            ),
        ledgerPreview = listOf(paymentRow, rentRow, lateFeeRow),
        canLogPayment = true,
        canAddCharge = true,
        canMove = true,
        calendar = populatedCalendar,
        lateFeeSuggestion =
            LateFeeSuggestionUiState(
                message = UiText.Plain("Rent for March is late. Add a $50.00 late fee?"),
                month = "2026-03",
                cents = 5_000,
            ),
    )

internal val emptyTenant =
    populatedTenant.copy(
        balance =
            TenantBalanceUiState(
                state = TenantBalanceState.PaidUp,
                total = UiText.Plain("$0.00"),
                lines = emptyList(),
                nextDue = null,
                partialMoveOutWarning = null,
            ),
        rentTerms = listOf(rentTerm("r1", "$1,500.00 a month · due the 15th", "From Mar 2026", isCurrent = true)),
        ledgerPreview = emptyList(),
        calendar = emptyCalendar,
        lateFeeSuggestion = null,
    )

internal val offlineTenant =
    populatedTenant.copy(
        canAddCharge = false,
        hasMenu = false,
        canEditDetails = false,
        lateFeeSuggestion = null,
        offlineBanner = UiText.Plain(OFFLINE_TEXT),
    )
