package net.neednot.renttracker.screenshots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.TenantChipState
import net.neednot.renttracker.core.designsystem.components.TenantStateChip
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
class TenantStateChipScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyTenantStateChipInLightTheme() =
        composeRule.captureScreenshot("tenant-state-chips", isDark = false) { Chips() }

    @Test
    fun everyTenantStateChipInDarkTheme() =
        composeRule.captureScreenshot("tenant-state-chips", isDark = true) { Chips() }
}

@Composable
private fun Chips() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TenantChipState.entries.forEach { state -> TenantStateChip(state = state) }
    }
}
