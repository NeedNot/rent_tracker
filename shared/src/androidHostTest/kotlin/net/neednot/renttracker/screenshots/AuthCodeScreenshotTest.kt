package net.neednot.renttracker.screenshots

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.presentation.screens.SignInCodeContent
import net.neednot.renttracker.feature.auth.presentation.uistate.SignInCodeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class AuthCodeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = code("auth-code-populated", populated, code = "481")

    @Test
    fun populatedInDarkTheme() = code("auth-code-populated", populated, code = "481", isDark = true)

    @Test
    fun errorInLightTheme() =
        code(
            "auth-code-error",
            populated.copy(error = UiText.Plain("That code is wrong or has expired. Request a new one.")),
            code = "481926",
        )

    @Test
    fun offlineInLightTheme() =
        code("auth-code-offline", populated.copy(offlineBanner = UiText.Plain(OFFLINE)), code = "481926")

    private fun code(
        name: String,
        uiState: SignInCodeUiState,
        code: String,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        SignInCodeContent(
            uiState = uiState,
            codeState = rememberTextFieldState(code),
            onVerifyClicked = {},
            onSendNewCodeClicked = {},
            onBackClicked = {},
        )
    }
}

private const val OFFLINE = "Offline · last updated just now"

private val populated = SignInCodeUiState(email = "you@example.com")
