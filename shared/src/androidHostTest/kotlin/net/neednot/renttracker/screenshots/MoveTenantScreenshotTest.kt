package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.presentation.screens.MoveTenantContent
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveConfirmationUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveTargetUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveTenantUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class MoveTenantScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = move("move-tenant", populatedMove)

    @Test
    fun populatedInDarkTheme() = move("move-tenant", populatedMove, isDark = true)

    @Test
    fun emptyInLightTheme() = move("move-tenant-empty", MoveTenantUiState.Empty)

    @Test
    fun errorInLightTheme() =
        move("move-tenant-error", MoveTenantUiState.Error(UiText.Plain("Something went wrong on our side.")))

    @Test
    fun confirmDialogInLightTheme() =
        composeRule.captureScreenWithDialog("move-tenant-confirm-dialog", isDark = false) {
            MoveTenantContent(
                uiState = populatedMove.copy(confirmation = confirmation),
                onTargetClicked = {},
                onMoveConfirmed = {},
                onConfirmationDismissed = {},
                onCloseClicked = {},
            )
        }

    private fun move(
        name: String,
        uiState: MoveTenantUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        MoveTenantContent(
            uiState = uiState,
            onTargetClicked = {},
            onMoveConfirmed = {},
            onConfirmationDismissed = {},
            onCloseClicked = {},
        )
    }
}

private val populatedMove =
    MoveTenantUiState.Content(
        tenantName = "Apt 3B",
        targets =
            listOf(
                MoveTargetUiState("g2", "Maple Court", UiText.Plain("4 tenants"), isSelected = true),
                MoveTargetUiState("g3", "Shared property", UiText.Plain("2 tenants"), isSelected = false),
            ),
        contactName = "Dana Whitfield",
        balance = UiText.Plain("Owes $550.00"),
    )

private val confirmation =
    MoveConfirmationUiState(
        title = UiText.Plain("Move Apt 3B to Maple Court?"),
        consequences =
            listOf(
                UiText.Plain("Members of 1st Street lose access to this tenant's ledger."),
                UiText.Plain("Members of Maple Court gain access."),
                UiText.Plain(
                    "Months are recalculated with Maple Court's settings: By date payment order, 3-day grace period.",
                ),
                UiText.Plain("The full history moves with the tenant."),
            ),
    )
