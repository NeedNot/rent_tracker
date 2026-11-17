package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.designsystem.components.CalendarState
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.presentation.screens.TenantActions
import net.neednot.renttracker.feature.tenants.presentation.screens.TenantContent
import net.neednot.renttracker.feature.tenants.presentation.uistate.AddFeeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.BalanceLineUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthDetailUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthLateFeeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthLateNoticeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthPaymentUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class MonthDetailScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = monthDetail("month-detail", partialMonth)

    @Test
    fun populatedInDarkTheme() = monthDetail("month-detail", partialMonth, isDark = true)

    @Test
    fun offlineInLightTheme() =
        monthDetail(
            "month-detail-offline",
            partialMonth.copy(
                lateNotice = partialMonth.lateNotice?.copy(addFee = null),
                offlineBanner = UiText.Plain(OFFLINE_TEXT),
            ),
        )

    @Test
    fun paidFeeUnpaidInLightTheme() = monthDetail("month-detail-fee-unpaid", paidFeeUnpaid)

    private fun monthDetail(
        name: String,
        detail: MonthDetailUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreenWithDialog(name, isDark) {
        TenantContent(
            uiState = populatedTenant.copy(lateFeeSuggestion = null, monthDetail = detail),
            actions = TenantActions(),
        )
    }
}

private val partialMonth =
    MonthDetailUiState(
        title = "March 2026",
        state = CalendarState.Partial,
        stateLabel = UiText.Plain("Partial"),
        rentLabel = UiText.Plain("Rent, due Mar 1"),
        rent = "$1,500.00",
        payments =
            listOf(
                MonthPaymentUiState(
                    title = UiText.Plain("Mar 2 payment · Zelle"),
                    amount = "$1,000.00",
                    notes = listOf(UiText.Plain("The other $500.00 of this $1,500.00 payment finished February.")),
                ),
            ),
        coverageNote = null,
        totals =
            listOf(
                BalanceLineUiState(UiText.Plain("Covered"), "$1,000.00"),
                BalanceLineUiState(UiText.Plain("Still owed"), "$500.00"),
            ),
        lateNotice =
            MonthLateNoticeUiState(
                title = UiText.Plain("Late · no late fee added"),
                message = UiText.Plain("Only $1,000.00 was paid by Mar 6, the end of the grace period."),
                addFee = AddFeeUiState("2026-03", 5_000),
            ),
        lateFee = null,
    )

private val paidFeeUnpaid =
    MonthDetailUiState(
        title = "February 2026",
        state = CalendarState.Paid,
        stateLabel = UiText.Plain("Paid"),
        rentLabel = UiText.Plain("Rent, due Feb 1"),
        rent = "$1,500.00",
        payments =
            listOf(
                MonthPaymentUiState(UiText.Plain("Feb 16 payment · Check"), "$1,000.00", emptyList()),
                MonthPaymentUiState(
                    title = UiText.Plain("Mar 2 payment · Zelle"),
                    amount = "$500.00",
                    notes = listOf(UiText.Plain("$500.00 of this $1,500.00 payment.")),
                ),
            ),
        coverageNote = null,
        totals = listOf(BalanceLineUiState(UiText.Plain("Covered"), "$1,500.00")),
        lateNotice = null,
        lateFee =
            MonthLateFeeUiState(
                title = UiText.Plain("Late fee for February · unpaid"),
                amount = "$50.00",
                notes = listOf(UiText.Plain("Added Feb 6 by Sam Rivera.")),
                isUnpaid = true,
            ),
    )
