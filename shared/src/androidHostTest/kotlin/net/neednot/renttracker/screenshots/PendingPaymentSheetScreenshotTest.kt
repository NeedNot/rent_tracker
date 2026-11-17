package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.designsystem.components.RtBottomSheet
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerItemActions
import net.neednot.renttracker.feature.tenants.presentation.components.PendingPaymentSheet
import net.neednot.renttracker.feature.tenants.presentation.screens.LedgerContent
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerFieldUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSectionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChangeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingMatchUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetBody
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class PendingPaymentSheetScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pendingInLightTheme() =
        pending(
            "entry-sheet-pending",
            PendingChip.Pending,
            PendingSheetBody.Waiting(
                message =
                    UiText.Plain(
                        "Not sent yet. It doesn't count toward the balance until the server accepts it.",
                    ),
                fields = listOf(field("Date received", "Mar 10, 2026"), field("Method", "Zelle")),
            ),
            listOf(PendingSheetAction.Edit, PendingSheetAction.Cancel),
        )

    @Test
    fun possibleDuplicateInLightTheme() =
        pending(
            "entry-sheet-possible-duplicate",
            PendingChip.NeedsConfirmation,
            PendingSheetBody.PossibleDuplicate(
                title = UiText.Plain("Possible duplicate: save anyway?"),
                intro = UiText.Plain("Apt 3B already has a payment of the same amount within 3 days:"),
                matches =
                    listOf(
                        PendingMatchUiState(
                            parts = listOf(UiText.Plain("Zelle"), UiText.Plain("Mar 10"), UiText.Plain("Sam Rivera")),
                            amount = "$550.00",
                        ),
                    ),
            ),
            listOf(PendingSheetAction.SaveAnyway, PendingSheetAction.CancelPayment),
        )

    @Test
    fun tenantChangedInLightTheme() =
        pending(
            "entry-sheet-tenant-changed",
            PendingChip.NeedsConfirmation,
            PendingSheetBody.TenantChanged(
                title = UiText.Plain("This tenant was updated while you were offline"),
                intro = UiText.Plain("Check these changes before sending:"),
                changes = listOf(PendingChangeUiState.Balance(UiText.Plain("Owes $550.00"), UiText.Plain("$0.00"))),
            ),
            listOf(PendingSheetAction.SendAnyway, PendingSheetAction.Edit, PendingSheetAction.CancelPayment),
        )

    @Test
    fun rejectedInLightTheme() =
        pending(
            "entry-sheet-rejected",
            PendingChip.Rejected,
            PendingSheetBody.Rejected(
                reason = UiText.Plain("You no longer have permission to log payments in this group."),
                notSaved =
                    UiText.Plain(
                        "This payment was not saved. If it was received, ask someone who can log payments to add it.",
                    ),
            ),
            listOf(PendingSheetAction.Dismiss),
        )

    private fun pending(
        name: String,
        chip: PendingChip,
        body: PendingSheetBody,
        actions: List<PendingSheetAction>,
    ) = composeRule.captureScreenWithDialog(name, isDark = false) {
        LedgerContent(
            uiState = backdrop,
            onBackClicked = {},
            onLogPaymentClicked = {},
            itemActions = LedgerItemActions.None,
        )
        RtBottomSheet(onDismissed = {}) {
            PendingPaymentSheet(
                sheet =
                    PendingSheetUiState(
                        clientId = "c1",
                        title = UiText.Plain("Payment"),
                        chip = chip,
                        amount = "$550.00",
                        body = body,
                        actions = actions,
                    ),
                isWorking = false,
                onActionClicked = {},
            )
        }
    }
}

private fun field(
    label: String,
    value: String,
) = LedgerFieldUiState(UiText.Plain(label), UiText.Plain(value))

private val backdrop =
    LedgerUiState.Content(
        tenantName = "Apt 3B",
        groupName = "1st Street",
        sections =
            listOf(
                LedgerSectionUiState("March 2026", listOf(pendingRow(PendingChip.Pending, "Saved on this phone"))),
            ),
        emptyMessage = null,
        canLogPayment = true,
    )
