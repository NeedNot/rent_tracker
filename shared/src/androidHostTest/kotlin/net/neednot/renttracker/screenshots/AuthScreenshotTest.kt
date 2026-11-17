package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.presentation.screens.DisplayNameContent
import net.neednot.renttracker.feature.auth.presentation.screens.SignInContent
import net.neednot.renttracker.feature.auth.presentation.uistate.DisplayNameUiState
import net.neednot.renttracker.feature.auth.presentation.uistate.SignInUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class AuthScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun signInPopulatedInLightTheme() = signIn("auth-sign-in-populated", signInPopulated)

    @Test
    fun signInPopulatedInDarkTheme() = signIn("auth-sign-in-populated", signInPopulated, isDark = true)

    @Test
    fun signInErrorInLightTheme() =
        signIn(
            "auth-sign-in-error",
            SignInUiState(
                email = "you@example",
                emailError = UiText.Plain("Enter a full email address, like name@example.com."),
            ),
        )

    @Test
    fun signInOfflineInLightTheme() = signIn("auth-sign-in-offline", signInPopulated.copy(offlineBanner = offline))

    @Test
    fun displayNamePopulatedInLightTheme() = displayName("auth-display-name-populated", displayNamePopulated)

    @Test
    fun displayNamePopulatedInDarkTheme() =
        displayName("auth-display-name-populated", displayNamePopulated, isDark = true)

    @Test
    fun displayNameErrorInLightTheme() =
        displayName(
            "auth-display-name-error",
            DisplayNameUiState(displayName = "", error = UiText.Plain("Enter a name, like Sam Rivera.")),
        )

    @Test
    fun displayNameOfflineInLightTheme() =
        displayName("auth-display-name-offline", displayNamePopulated.copy(offlineBanner = offline))

    private fun signIn(
        name: String,
        uiState: SignInUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        SignInContent(uiState = uiState, onEmailChanged = {}, onSendCodeClicked = {}, onGoogleSignInClicked = {})
    }

    private fun displayName(
        name: String,
        uiState: DisplayNameUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        DisplayNameContent(uiState = uiState, onDisplayNameChanged = {}, onContinueClicked = {})
    }
}

private val offline = UiText.Plain("Offline · last updated just now")

private val signInPopulated = SignInUiState(email = "you@example.com")

private val displayNamePopulated = DisplayNameUiState(displayName = "Need_Not")
