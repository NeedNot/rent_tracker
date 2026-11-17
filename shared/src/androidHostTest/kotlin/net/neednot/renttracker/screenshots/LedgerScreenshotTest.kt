package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerItemActions
import net.neednot.renttracker.feature.tenants.presentation.screens.LedgerContent
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSectionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChip
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class LedgerScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1300dp-xhdpi")
    fun populatedInLightTheme() = ledger("ledger-populated", populatedLedger)

    @Test
    fun populatedInDarkTheme() = ledger("ledger-populated", populatedLedger, isDark = true)

    @Test
    fun offlineInLightTheme() = ledger("ledger-offline", offlineLedger)

    @Test
    fun emptyInLightTheme() =
        ledger(
            "ledger-empty",
            populatedLedger.copy(
                sections = emptyList(),
                emptyMessage = UiText.Plain("Payments and charges you add will show here."),
            ),
        )

    @Test
    fun errorInLightTheme() =
        ledger("ledger-error", LedgerUiState.Error(UiText.Plain("Something went wrong on our side.")))

    private fun ledger(
        name: String,
        uiState: LedgerUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        LedgerContent(
            uiState = uiState,
            onBackClicked = {},
            onLogPaymentClicked = {},
            itemActions = LedgerItemActions.None,
        )
    }
}

private val march = LedgerSectionUiState("March 2026", listOf(paymentRow, rentRow))

private val february =
    LedgerSectionUiState(
        "February 2026",
        listOf(
            ledgerRow("Feb 25", "Refunded credit", "$300.00", "Owes $550.00", listOf("Custom debt · added by you")),
            ledgerRow(
                "Feb 16",
                "Payment · Check",
                "$1,400.00",
                details = listOf("Feb 19 by you"),
                chips = listOf(EntryChip.Bounced),
                isStruckThrough = true,
                canRestore = true,
            ),
            lateFeeRow,
            ledgerRow(
                "Feb 4",
                "Payment · Check",
                "$1,000.00",
                details = listOf("Entered twice · Sam Rivera"),
                chips = listOf(EntryChip.Cancelled),
                isStruckThrough = true,
                canRestore = true,
            ),
            ledgerRow("Feb 1", "Rent · February", "$1,500.00", "Owes $1,570.00"),
        ),
    )

private val january =
    LedgerSectionUiState(
        "January 2026",
        listOf(
            ledgerRow(
                "Jan 21",
                "Late fee · for January",
                "$70.00",
                "Owes $70.00",
                listOf("Added by Sam Rivera"),
                mayNotApply = true,
            ),
            ledgerRow(
                "Jan 3",
                "Payment · Zelle",
                "Paid $1,500.00",
                "Owes $1,400.00",
                listOf("by you · Jan 4"),
                chips = listOf(EntryChip.Edited),
                isPaidAmount = true,
            ),
        ),
    )

private val populatedLedger =
    LedgerUiState.Content(
        tenantName = "Apt 3B",
        groupName = "1st Street",
        sections = listOf(march, february, january),
        emptyMessage = null,
        canLogPayment = true,
    )

private val offlineLedger =
    populatedLedger.copy(
        sections =
            listOf(
                march.copy(
                    rows =
                        listOf(
                            pendingRow(PendingChip.Pending, "Saved on this phone"),
                            pendingRow(PendingChip.NeedsConfirmation, "Possible duplicate"),
                            pendingRow(PendingChip.Rejected, "Not saved"),
                        ) + march.rows,
                ),
                february,
            ),
        offlineBanner = UiText.Plain("Offline · last updated 40 min ago"),
        pendingNote = UiText.Plain("Rows not yet on the server never change the running balance."),
    )
