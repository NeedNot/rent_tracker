package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.presentation.screens.GroupSettingsActions
import net.neednot.renttracker.feature.group.presentation.screens.GroupSettingsContent
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsField
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsForm
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsUiState
import net.neednot.renttracker.feature.group.presentation.uistate.TimeZoneOptionUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class GroupSettingsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsInLightTheme() = settings("group-settings", editing, isDark = false)

    @Test
    fun settingsInDarkTheme() = settings("group-settings", editing, isDark = true)

    @Test
    fun settingsReadOnlyInLightTheme() = settings("group-settings-read-only", readOnly)

    @Test
    fun settingsReadOnlyInDarkTheme() = settings("group-settings-read-only", readOnly, isDark = true)

    @Test
    fun settingsConflictInLightTheme() = settings("group-settings-conflict", conflict)

    @Test
    fun settingsErrorInLightTheme() =
        settings("group-settings-error", GroupSettingsUiState.Error(UiText.Plain("This group no longer exists.")))

    private fun settings(
        name: String,
        uiState: GroupSettingsUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        GroupSettingsContent(uiState = uiState, actions = GroupSettingsActions())
    }
}

private val timeZones =
    listOf(TimeZoneOptionUiState("America/Los_Angeles", UiText.Plain("Pacific Time (Los Angeles)")))

private val form = GroupSettingsForm("1st Street", "America/Los_Angeles", PaymentOrder.RentFirst, "5", "50.00")

private val editing = GroupSettingsUiState.Editing(form = form, timeZoneOptions = timeZones)

private val readOnly = GroupSettingsUiState.ReadOnly(form = form, timeZoneOptions = timeZones)

private val conflict =
    editing.copy(
        form = form.copy(name = "", graceDays = "20"),
        conflictTitle = UiText.Plain("Someone else changed this"),
        fieldErrors =
            mapOf(
                GroupSettingsField.Name to UiText.Plain("Enter a name for the group."),
                GroupSettingsField.GraceDays to UiText.Plain("Enter a number from 0 to 15."),
            ),
    )
