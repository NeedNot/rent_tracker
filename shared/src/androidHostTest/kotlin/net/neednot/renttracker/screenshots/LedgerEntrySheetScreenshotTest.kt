package net.neednot.renttracker.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.designsystem.components.RtBottomSheet
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerEntryDialog
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerEntrySheet
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerItemActions
import net.neednot.renttracker.feature.tenants.presentation.screens.LedgerContent
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryEditTarget
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerDialogUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerFieldUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSectionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class LedgerEntrySheetScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun paymentInLightTheme() = entry("entry-sheet-payment", paymentSheet)

    @Test
    fun paymentInDarkTheme() = entry("entry-sheet-payment", paymentSheet, isDark = true)

    @Test
    fun chargeInLightTheme() = entry("entry-sheet-charge", chargeSheet)

    @Test
    fun bouncedPaymentInLightTheme() = entry("entry-sheet-bounced", bouncedSheet)

    @Test
    fun readOnlyMemberInLightTheme() = entry("entry-sheet-read-only", paymentSheet.copy(actions = emptyList()))

    @Test
    fun markBouncedDialogInLightTheme() =
        entry(
            "entry-mark-bounced-dialog",
            paymentSheet,
            LedgerDialogUiState.ConfirmBounced(
                UiText.Plain(
                    "It stays in the ledger crossed out and the $1,400.00 check from Feb 16 is owed again. " +
                        "You can add a returned-payment fee next.",
                ),
            ),
        )

    private fun entry(
        name: String,
        sheet: LedgerSheetUiState,
        dialog: LedgerDialogUiState? = null,
        isDark: Boolean = false,
    ) = composeRule.captureScreenWithDialog(name, isDark) { EntryOverLedger(sheet, dialog) }
}

@Composable
private fun EntryOverLedger(
    sheet: LedgerSheetUiState,
    dialog: LedgerDialogUiState?,
) {
    LedgerContent(
        uiState = sheetBackdrop,
        onBackClicked = {},
        onLogPaymentClicked = {},
        itemActions = LedgerItemActions.None,
    )
    RtBottomSheet(onDismissed = {}) { LedgerEntrySheet(sheet = sheet, isWorking = false, onActionClicked = {}) }
    dialog?.let { LedgerEntryDialog(dialog = it, onTextChanged = {}, onConfirmed = {}, onDismissed = {}) }
}

private val sheetBackdrop =
    LedgerUiState.Content(
        tenantName = "Apt 3B",
        groupName = "1st Street",
        sections = listOf(LedgerSectionUiState("March 2026", listOf(paymentRow, rentRow))),
        emptyMessage = null,
        canLogPayment = true,
    )

private fun field(
    label: String,
    value: String,
) = LedgerFieldUiState(UiText.Plain(label), UiText.Plain(value))

private val paymentSheet =
    LedgerSheetUiState(
        key = "payment:p1",
        title = UiText.Plain("Payment"),
        amount = "$1,500.00",
        chips = emptyList(),
        fields =
            listOf(
                field("Date received", "Mar 2, 2026"),
                field("Method", "Zelle"),
                field("Logged", "Mar 2, 2026 by you"),
            ),
        actions = listOf(LedgerSheetAction.Edit, LedgerSheetAction.MarkBounced, LedgerSheetAction.CancelPayment),
        editTarget = EntryEditTarget.Payment("p1"),
    )

private val chargeSheet =
    LedgerSheetUiState(
        key = "debt:d1",
        title = UiText.Plain("Late fee"),
        amount = "$50.00",
        chips = emptyList(),
        fields =
            listOf(
                field("For", "February 2026"),
                field("Date", "Feb 6, 2026"),
                field("Added", "Feb 6, 2026 by Sam Rivera"),
            ),
        actions = listOf(LedgerSheetAction.Edit, LedgerSheetAction.CancelCharge),
        editTarget = EntryEditTarget.Charge("d1"),
    )

private val bouncedSheet =
    LedgerSheetUiState(
        key = "payment:p2",
        title = UiText.Plain("Payment"),
        amount = "$1,400.00",
        chips = listOf(EntryChip.Bounced),
        fields =
            listOf(
                field("Date received", "Feb 16, 2026"),
                field("Method", "Check"),
                field("Marked bounced", "Feb 19, 2026 by you"),
            ),
        actions = listOf(LedgerSheetAction.Restore),
        editTarget = EntryEditTarget.Payment("p2"),
    )
