package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.presentation.screens.TransferOwnershipActions
import net.neednot.renttracker.feature.members.presentation.screens.TransferOwnershipContent
import net.neednot.renttracker.feature.members.presentation.uistate.TransferCandidateUiState
import net.neednot.renttracker.feature.members.presentation.uistate.TransferConfirmUiState
import net.neednot.renttracker.feature.members.presentation.uistate.TransferOwnershipUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class TransferOwnershipScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = transfer("transfer-ownership", populated)

    @Test
    fun populatedInDarkTheme() = transfer("transfer-ownership", populated, isDark = true)

    @Test
    fun emptyInLightTheme() =
        transfer("transfer-ownership-empty", TransferOwnershipUiState(isLoading = false, groupName = "1st Street"))

    @Test
    fun errorInLightTheme() =
        transfer(
            "transfer-ownership-error",
            TransferOwnershipUiState(isLoading = false, error = UiText.Plain("Something went wrong on our side.")),
        )

    @Test
    fun confirmDialogInLightTheme() =
        composeRule.captureScreenWithDialog("transfer-ownership-confirm", isDark = false) {
            TransferOwnershipContent(
                uiState =
                    populated.copy(confirm = TransferConfirmUiState("m2", "Sam Rivera", "Sam", "1st Street")),
                actions = TransferOwnershipActions(),
            )
        }

    private fun transfer(
        name: String,
        uiState: TransferOwnershipUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        TransferOwnershipContent(uiState = uiState, actions = TransferOwnershipActions())
    }
}

private val populated =
    TransferOwnershipUiState(
        isLoading = false,
        groupName = "1st Street",
        candidates =
            listOf(
                TransferCandidateUiState("m2", "SR", "Sam Rivera", "sam.rivera@example.com"),
                TransferCandidateUiState("m4", "ML", "Maria Lopez", "maria.lopez@example.com"),
            ),
    )
