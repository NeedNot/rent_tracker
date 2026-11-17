package net.neednot.renttracker.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.presentation.components.InvitationActions
import net.neednot.renttracker.feature.home.presentation.components.RejectedPaymentActions
import net.neednot.renttracker.feature.home.presentation.screens.HomeContent
import net.neednot.renttracker.feature.home.presentation.uistate.GroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState
import net.neednot.renttracker.feature.home.presentation.uistate.InvitationCardUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class HomeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = composeRule.captureScreen("home-populated", isDark = false) { Home(populated) }

    @Test
    fun populatedInDarkTheme() = composeRule.captureScreen("home-populated", isDark = true) { Home(populated) }

    @Test
    fun offlineInLightTheme() = composeRule.captureScreen("home-offline", isDark = false) { Home(offline) }

    @Test
    fun emptyInLightTheme() = composeRule.captureScreen("home-empty", isDark = false) { Home(empty) }

    @Test
    fun errorInLightTheme() = composeRule.captureScreen("home-error", isDark = false) { Home(error) }
}

@Composable
private fun Home(uiState: HomeUiState) {
    HomeContent(
        uiState = uiState,
        invitationActions = InvitationActions.None,
        rejectedPaymentActions = RejectedPaymentActions.None,
        onGroupClicked = {},
        onNewGroupClicked = {},
        onArchivedGroupsClicked = {},
        onAccountClicked = {},
    )
}

private val populated =
    HomeUiState.Content(
        accountInitial = "N",
        invitations =
            listOf(
                InvitationCardUiState(
                    inviteId = "i1",
                    groupName = "Oak Ave Duplex",
                    summary = UiText.Plain("Sam Rivera invited you as Bookkeeper"),
                    description = UiText.Plain("Invitation to Oak Ave Duplex"),
                    isWorking = false,
                ),
            ),
        groups =
            listOf(
                GroupCardUiState(
                    groupId = "g1",
                    name = "1st Street",
                    balance = UiText.Plain("Owes $3,900.00"),
                    isPaidUp = false,
                    tenantCount = UiText.Plain("6 tenants"),
                    lateCount = UiText.Plain("2 late"),
                    archivedOwed = UiText.Plain("Archived tenants owe $2,400.00"),
                ),
                GroupCardUiState(
                    groupId = "g2",
                    name = "Maple Court",
                    balance = UiText.Plain("Paid up"),
                    isPaidUp = true,
                    tenantCount = UiText.Plain("4 tenants · none late"),
                    lateCount = null,
                    archivedOwed = null,
                ),
            ),
        archivedGroupsRow = UiText.Plain("Archived groups (1)"),
    )

private val offline =
    populated.copy(invitations = emptyList(), offlineBanner = UiText.Plain("Offline · last updated 2h ago"))

private val empty = HomeUiState.Empty(accountInitial = "N")

private val error =
    HomeUiState.Error(message = UiText.Plain("Something went wrong on our side."), accountInitial = "N")
