package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.presentation.screens.PaymentEditorActions
import net.neednot.renttracker.feature.payments.presentation.screens.PaymentEditorContent
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
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
class PaymentEditorScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = paymentEditor("payment-editor", paymentEditorPopulated)

    @Test
    fun populatedInDarkTheme() = paymentEditor("payment-editor", paymentEditorPopulated, isDark = true)

    @Test
    fun lateFeeCheckedInLightTheme() =
        paymentEditor(
            "payment-editor-late-fee",
            paymentEditorPopulated.copy(lateFeeOffer = paymentEditorPopulated.lateFeeOffer?.copy(isChecked = true)),
        )

    @Test
    fun editPaymentInLightTheme() =
        paymentEditor(
            "payment-editor-edit",
            paymentEditorPopulated.copy(isNewPayment = false, lateFeeOffer = null),
        )

    @Test
    fun offlineInLightTheme() =
        paymentEditor(
            "payment-editor-offline",
            paymentEditorPopulated.copy(offlineBanner = UiText.Plain("Offline · last updated 2h ago")),
        )

    @Test
    fun errorInLightTheme() =
        paymentEditor(
            "payment-editor-error",
            paymentEditorPopulated.copy(
                amount = "15OO",
                date = LocalDate.parse("2026-03-12"),
                error = UiText.Plain("Something went wrong on our side. Try again."),
                fieldErrors =
                    mapOf(
                        PaymentEditorField.Amount to UiText.Plain("Enter dollars and cents, like 1,500.00."),
                        PaymentEditorField.Date to UiText.Plain("Can't be later than today, Mar 10, 2026."),
                    ),
            ),
        )

    @Test
    fun editConflictInLightTheme() =
        paymentEditor(
            "payment-editor-conflict",
            paymentEditorPopulated.copy(
                isNewPayment = false,
                lateFeeOffer = null,
                conflictBanner = UiText.Plain("Someone else changed this"),
            ),
        )

    private fun paymentEditor(
        name: String,
        uiState: PaymentEditorUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        PaymentEditorContent(uiState = uiState, actions = PaymentEditorActions())
    }
}
