package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.account.presentation.screens.AccountActions
import net.neednot.renttracker.feature.account.presentation.screens.AccountContent
import net.neednot.renttracker.feature.account.presentation.uistate.AccountUiState
import net.neednot.renttracker.feature.account.presentation.uistate.DeletionRowUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class AccountScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = account("account-populated", populated)

    @Test
    fun populatedInDarkTheme() = account("account-populated", populated, isDark = true)

    @Test
    fun deletionRequestedInLightTheme() =
        account(
            "account-deletion-requested",
            populated.copy(
                deletion = DeletionRowUiState.Requested(UiText.Plain("Requested Nov 14. Our team will handle it.")),
            ),
        )

    @Test
    fun offlineInLightTheme() =
        account("account-offline", populated.copy(offlineBanner = UiText.Plain("Offline · last updated 2h ago")))

    @Test
    fun errorInLightTheme() =
        account("account-error", AccountUiState.Error(UiText.Plain("Something went wrong on our side.")))

    @Test
    fun signOutDialogInLightTheme() = dialog("account-sign-out-dialog", populated.copy(unsyncedPaymentsOnSignOut = 0))

    @Test
    fun signOutUnsyncedPaymentDialogInLightTheme() =
        dialog("account-sign-out-unsynced-payment-dialog", populated.copy(unsyncedPaymentsOnSignOut = 1))

    @Test
    fun requestDeletionDialogInLightTheme() =
        dialog("account-request-deletion-dialog", populated.copy(isDeleteDialogShown = true))

    private fun account(
        name: String,
        uiState: AccountUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) { AccountContent(uiState = uiState, actions = AccountActions()) }

    private fun dialog(
        name: String,
        uiState: AccountUiState,
    ) = composeRule.captureScreenWithDialog(name, isDark = false) {
        AccountContent(uiState = uiState, actions = AccountActions())
    }
}

private val populated =
    AccountUiState.Content(
        initials = "N",
        displayName = "Need_Not",
        email = "you@example.com",
        deletion = DeletionRowUiState.Available,
    )
