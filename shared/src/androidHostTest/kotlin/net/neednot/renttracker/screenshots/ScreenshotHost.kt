package net.neednot.renttracker.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme

private const val GOLDEN_DIRECTORY = "src/androidHostTest/screenshots"

internal const val PHONE_QUALIFIERS = "w390dp-h844dp-xhdpi"

internal fun ComposeContentTestRule.captureScreenshot(
    name: String,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    captureScreen(name, isDark) { Box(modifier = Modifier.padding(16.dp)) { content() } }
}

internal fun ComposeContentTestRule.captureScreen(
    name: String,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    setThemedContent(isDark, content)
    onRoot().captureRoboImage(goldenPath(name, isDark))
}

@OptIn(ExperimentalRoborazziApi::class)
internal fun ComposeContentTestRule.captureScreenWithDialog(
    name: String,
    isDark: Boolean,
    beforeCapture: ComposeContentTestRule.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    setThemedContent(isDark, content)
    beforeCapture()
    waitForIdle()
    captureScreenRoboImage(goldenPath(name, isDark))
}

private fun ComposeContentTestRule.setThemedContent(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    setContent {
        RentTrackerTheme(darkTheme = isDark) {
            Surface(color = MaterialTheme.colorScheme.background, content = content)
        }
    }
}

private fun goldenPath(
    name: String,
    isDark: Boolean,
): String = "$GOLDEN_DIRECTORY/$name-${if (isDark) "dark" else "light"}.png"
