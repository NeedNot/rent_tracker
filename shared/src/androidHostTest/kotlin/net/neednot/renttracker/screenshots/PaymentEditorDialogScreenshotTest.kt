package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.presentation.screens.PaymentEditorActions
import net.neednot.renttracker.feature.payments.presentation.screens.PaymentEditorContent
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateDialogUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.DuplicateMatchUiState
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class PaymentEditorDialogScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun possibleDuplicateInLightTheme() =
        paymentDialog(
            "payment-editor-duplicate",
            withDuplicate,
        )

    @Test
    fun possibleDuplicateInDarkTheme() =
        paymentDialog(
            "payment-editor-duplicate",
            withDuplicate,
            isDark = true,
        )

    @Test
    fun firstOfflineSaveInLightTheme() =
        paymentDialog(
            "payment-editor-saved-on-phone",
            paymentEditorPopulated.copy(
                lateFeeOffer = null,
                offlineBanner = UiText.Plain("Offline · last updated 2h ago"),
                isSyncAlertsExplainerShown = true,
            ),
        )

    private fun paymentDialog(
        name: String,
        uiState: PaymentEditorUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreenWithDialog(name, isDark) {
        PaymentEditorContent(uiState = uiState, actions = PaymentEditorActions())
    }
}

private val withDuplicate =
    paymentEditorPopulated.copy(
        duplicate =
            DuplicateDialogUiState(
                title = UiText.Plain("Possible duplicate"),
                intro = UiText.Plain("A payment like this was already logged for Apt 3B."),
                matches =
                    listOf(
                        DuplicateMatchUiState(
                            kind = null,
                            date = "Mar 2",
                            method = PaymentMethod.Zelle,
                            amount = UiText.Plain("Paid $1,500.00"),
                        ),
                    ),
            ),
    )
