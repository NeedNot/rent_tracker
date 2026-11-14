package net.neednot.renttracker.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.designsystem.components.CalendarState
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.presentation.components.CalendarGrid
import net.neednot.renttracker.feature.tenants.presentation.uistate.CalendarSquareUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_fee_marker
import net.neednot.renttracker.resources.calendar_square_owed
import net.neednot.renttracker.resources.calendar_state_due_not_late
import net.neednot.renttracker.resources.calendar_state_not_a_tenant
import net.neednot.renttracker.resources.calendar_state_owed
import net.neednot.renttracker.resources.calendar_state_paid
import net.neednot.renttracker.resources.calendar_state_partial
import net.neednot.renttracker.resources.calendar_state_prepaid
import net.neednot.renttracker.resources.calendar_state_upcoming
import org.jetbrains.compose.resources.StringResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class CalendarGridScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyCalendarStateInLightTheme() = composeRule.captureScreenshot("calendar-grid", isDark = false) { Grid() }

    @Test
    fun everyCalendarStateInDarkTheme() = composeRule.captureScreenshot("calendar-grid", isDark = true) { Grid() }
}

@Composable
private fun Grid() {
    CalendarGrid(squares = squares(), onMonthClicked = {})
}

private fun squares(): List<CalendarSquareUiState> =
    listOf(
        square("Jan", CalendarState.Paid, Res.string.calendar_state_paid),
        square("Feb", CalendarState.Paid, Res.string.calendar_state_paid, fee = "$50.00"),
        square("Mar", CalendarState.Partial, Res.string.calendar_state_partial, owed = "$500.00"),
        square("Apr", CalendarState.Owed, Res.string.calendar_state_owed, owed = "$1,500.00"),
        square("May", CalendarState.Owed, Res.string.calendar_state_owed, owed = "$1,500.00", fee = "$50.00"),
        square("Jun", CalendarState.DueNotLate, Res.string.calendar_state_due_not_late),
        square("Jul", CalendarState.Upcoming, Res.string.calendar_state_upcoming),
        square("Aug", CalendarState.Prepaid, Res.string.calendar_state_prepaid),
        square("Sep", CalendarState.NotATenant, Res.string.calendar_state_not_a_tenant),
        square("Oct", CalendarState.Partial, Res.string.calendar_state_partial, owed = "$500.00", fee = "$50.00"),
        square("Nov", CalendarState.Upcoming, Res.string.calendar_state_upcoming),
        square("Dec", CalendarState.Upcoming, Res.string.calendar_state_upcoming),
    )

private fun square(
    monthName: String,
    state: CalendarState,
    label: StringResource,
    owed: String? = null,
    fee: String? = null,
): CalendarSquareUiState =
    CalendarSquareUiState(
        month = monthName,
        monthName = monthName,
        state = state,
        stateLabel = UiText.Resource(label),
        owed = owed?.let { UiText.Resource(Res.string.calendar_square_owed, listOf(it)) },
        feeMarker = fee?.let { UiText.Resource(Res.string.calendar_fee_marker, listOf(it)) },
        description = listOf(UiText.Plain(monthName), UiText.Resource(label)),
        isTappable = state != CalendarState.NotATenant,
    )
