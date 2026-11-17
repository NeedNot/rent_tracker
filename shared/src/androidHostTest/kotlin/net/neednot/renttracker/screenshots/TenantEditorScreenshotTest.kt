package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.presentation.screens.RentTermEditorActions
import net.neednot.renttracker.feature.tenants.presentation.screens.RentTermEditorContent
import net.neednot.renttracker.feature.tenants.presentation.screens.TenantEditorContent
import net.neednot.renttracker.feature.tenants.presentation.uistate.PastMonthWarningUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorForm
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantEditorUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class TenantEditorScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun newTenantInLightTheme() = tenantEditor("tenant-editor", isDark = false)

    @Test
    fun newTenantInDarkTheme() = tenantEditor("tenant-editor", isDark = true)

    @Test
    fun rentTermInLightTheme() =
        composeRule.captureScreen("rent-term-editor", isDark = false) {
            RentTermEditorContent(uiState = rentTerm, actions = RentTermEditorActions())
        }

    @Test
    fun rentTermInDarkTheme() =
        composeRule.captureScreen("rent-term-editor", isDark = true) {
            RentTermEditorContent(uiState = rentTerm, actions = RentTermEditorActions())
        }

    private fun tenantEditor(
        name: String,
        isDark: Boolean,
    ) = composeRule.captureScreen(name, isDark) {
        TenantEditorContent(
            uiState =
                TenantEditorUiState(
                    isNewTenant = true,
                    form =
                        TenantEditorForm(
                            name = "Apt 3B",
                            contactName = "Dana Whitfield",
                            monthlyRent = "1,500.00",
                            dueDay = "1",
                            moveIn = LocalDate.parse("2024-01-01"),
                        ),
                ),
            onFieldChanged = { _, _ -> },
            onMoveInChanged = {},
            onLastDayInUnitChanged = {},
            onSaveClicked = {},
            onCloseClicked = {},
        )
    }
}

private val rentTerm =
    RentTermEditorUiState(
        isNewTerm = false,
        isLoading = false,
        startMonth = YearMonth(2026, 1),
        amount = "1,500.00",
        dueDay = "15",
        pastMonthWarning =
            PastMonthWarningUiState.BalanceChange(
                before = UiText.Plain("Owes $550.00"),
                after = UiText.Plain("Owes $650.00"),
            ),
        showsDueDayReminder = true,
        canDelete = true,
    )
