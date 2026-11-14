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
import com.github.takahirom.roborazzi.captureRoboImage
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme

private const val GOLDEN_DIRECTORY = "src/androidHostTest/screenshots"

internal fun ComposeContentTestRule.captureScreenshot(
    name: String,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    setContent {
        RentTrackerTheme(darkTheme = isDark) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.padding(16.dp)) { content() }
            }
        }
    }
    onRoot().captureRoboImage("$GOLDEN_DIRECTORY/$name-${if (isDark) "dark" else "light"}.png")
}
