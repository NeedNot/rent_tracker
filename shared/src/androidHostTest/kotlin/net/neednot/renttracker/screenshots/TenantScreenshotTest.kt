package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.presentation.screens.TenantActions
import net.neednot.renttracker.feature.tenants.presentation.screens.TenantContent
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingBannerUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantConfirmationUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusChangeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val TALL_PHONE_QUALIFIERS = "w390dp-h2200dp-xhdpi"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class TenantScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIERS)
    fun populatedInLightTheme() = tenant("tenant-populated", populatedTenant)

    @Test
    fun populatedInDarkTheme() = tenant("tenant-populated", populatedTenant, isDark = true)

    @Test
    fun emptyInLightTheme() = tenant("tenant-empty", emptyTenant)

    @Test
    fun offlineInLightTheme() = tenant("tenant-offline", offlineTenant)

    @Test
    fun errorInLightTheme() =
        tenant("tenant-error", TenantUiState.Error(UiText.Plain("Something went wrong on our side.")))

    @Test
    fun paymentNeedsAttentionInLightTheme() = tenant("tenant-payment-needs-attention", needsAttention)

    @Test
    fun paymentWaitingToSyncInLightTheme() = tenant("tenant-payment-waiting", waitingToSync)

    @Test
    fun menuInLightTheme() =
        composeRule.captureScreenWithDialog(
            "tenant-menu",
            isDark = false,
            beforeCapture = { onNodeWithContentDescription("More options").performClick() },
        ) { TenantContent(uiState = populatedTenant.copy(lateFeeSuggestion = null), actions = TenantActions()) }

    @Test
    fun archiveDialogInLightTheme() =
        composeRule.captureScreenWithDialog("tenant-archive-dialog", isDark = false) {
            TenantContent(uiState = archiveDialog, actions = TenantActions())
        }

    private fun tenant(
        name: String,
        uiState: TenantUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) { TenantContent(uiState = uiState, actions = TenantActions()) }
}

private val needsAttention =
    populatedTenant.copy(
        lateFeeSuggestion = null,
        pendingBanner =
            PendingBannerUiState.Attention(
                title = UiText.Plain("1 payment needs your attention"),
                message = UiText.Plain("Open it in the ledger to confirm or fix it."),
                reviewClientId = "c1",
            ),
        ledgerPreview = listOf(pendingRow(PendingChip.NeedsConfirmation, "Possible duplicate"), paymentRow, rentRow),
    )

private val waitingToSync =
    populatedTenant.copy(
        lateFeeSuggestion = null,
        pendingBanner = PendingBannerUiState.Waiting(UiText.Plain("1 payment waiting to sync")),
        ledgerPreview = listOf(pendingRow(PendingChip.Pending, "Saved on this phone"), paymentRow, rentRow),
    )

private val archiveDialog =
    populatedTenant.copy(
        lateFeeSuggestion = null,
        statusChange =
            TenantStatusChangeUiState(
                confirmation =
                    TenantConfirmationUiState(
                        action = TenantStatusAction.Archive,
                        message = UiText.Plain("Apt 3B still owes $550.00. Archive anyway?"),
                    ),
            ),
    )
