package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.presentation.screens.ArchivedTenantsContent
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantRowUiState
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class ArchivedTenantsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun archivedTenantsInLightTheme() = archivedTenants("archived-tenants", archivedTenantList, isDark = false)

    @Test
    fun archivedTenantsInDarkTheme() = archivedTenants("archived-tenants", archivedTenantList, isDark = true)

    @Test
    fun archivedTenantsEmptyInLightTheme() = archivedTenants("archived-tenants-empty", ArchivedTenantsUiState.Empty())

    private fun archivedTenants(
        name: String,
        uiState: ArchivedTenantsUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        ArchivedTenantsContent(uiState = uiState, onBackClicked = {}, onTenantClicked = {})
    }
}

private val archivedTenantList =
    ArchivedTenantsUiState.Content(
        tenants =
            listOf(
                ArchivedTenantRowUiState("t1", "Apt 4C", "Sam Park", UiText.Plain("Owes $2,400.00")),
                ArchivedTenantRowUiState("t2", "Apt 5A", null, UiText.Plain("Paid up")),
            ),
    )
