package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.debts.presentation.screens.ChargeEditorActions
import net.neednot.renttracker.feature.debts.presentation.screens.ChargeEditorContent
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorField
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorUiState
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeHeaderUiState
import net.neednot.renttracker.feature.debts.presentation.uistate.DuplicateLateFeeUiState
import net.neednot.renttracker.feature.debts.presentation.uistate.LateFeeDuplicateUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class ChargeEditorScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lateFeeInLightTheme() = chargeEditor("charge-editor", lateFee)

    @Test
    fun lateFeeInDarkTheme() = chargeEditor("charge-editor", lateFee, isDark = true)

    @Test
    fun customDebtInLightTheme() = chargeEditor("charge-editor-custom", customDebt)

    @Test
    fun editChargeInLightTheme() = chargeEditor("charge-editor-edit", lateFee.copy(isNewCharge = false))

    @Test
    fun errorInLightTheme() =
        chargeEditor(
            "charge-editor-error",
            customDebt.copy(
                amount = "5O",
                note = "",
                error = UiText.Plain("Something went wrong on our side. Try again."),
                fieldErrors =
                    mapOf(
                        ChargeEditorField.Amount to UiText.Plain("Enter dollars and cents, like 50.00."),
                        ChargeEditorField.Note to UiText.Plain("Add a note that says what this charge is for."),
                    ),
            ),
        )

    @Test
    fun duplicateLateFeeInLightTheme() =
        composeRule.captureScreenWithDialog("charge-editor-duplicate", isDark = false) {
            ChargeEditorContent(
                uiState =
                    lateFee.copy(
                        duplicate =
                            LateFeeDuplicateUiState(
                                month = "March",
                                matches =
                                    listOf(
                                        DuplicateLateFeeUiState("Mar 7", UiText.Plain("Sam Rivera"), "$50.00"),
                                    ),
                            ),
                    ),
                actions = ChargeEditorActions(),
            )
        }

    private fun chargeEditor(
        name: String,
        uiState: ChargeEditorUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        ChargeEditorContent(uiState = uiState, actions = ChargeEditorActions())
    }
}

private val lateFee =
    ChargeEditorUiState(
        isNewCharge = true,
        isLoading = false,
        header =
            ChargeHeaderUiState(
                tenantName = "Apt 3B",
                groupName = "1st Street",
                balance = UiText.Plain("Owes $550.00"),
            ),
        today = LocalDate(2026, 3, 10),
        amount = "50.00",
        date = LocalDate(2026, 3, 10),
        forMonth = YearMonth(2026, 3),
    )

private val customDebt = lateFee.copy(kind = DebtKind.Custom, amount = "120.00")
