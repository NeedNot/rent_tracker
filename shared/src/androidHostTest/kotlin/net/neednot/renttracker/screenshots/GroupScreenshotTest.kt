package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.designsystem.components.TenantChipState
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.presentation.screens.GroupActions
import net.neednot.renttracker.feature.group.presentation.screens.GroupContent
import net.neednot.renttracker.feature.group.presentation.uistate.GroupChromeUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupMenuItem
import net.neednot.renttracker.feature.group.presentation.uistate.GroupUiState
import net.neednot.renttracker.feature.group.presentation.uistate.TenantRowUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class GroupScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = group("group-populated", populated, isDark = false)

    @Test
    fun populatedInDarkTheme() = group("group-populated", populated, isDark = true)

    @Test
    fun viewOnlyInLightTheme() = group("group-view-only", viewOnly)

    @Test
    fun archivedGroupInLightTheme() = group("group-archived", archivedGroup)

    @Test
    fun archivedGroupInDarkTheme() = group("group-archived", archivedGroup, isDark = true)

    @Test
    fun offlineInLightTheme() = group("group-offline", offline)

    @Test
    fun emptyInLightTheme() = group("group-empty", empty)

    @Test
    fun errorInLightTheme() =
        group("group-error", GroupUiState.Error(UiText.Plain("Something went wrong on our side.")))

    private fun group(
        name: String,
        uiState: GroupUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) { GroupContent(uiState = uiState, actions = GroupActions()) }
}

private fun tenant(
    name: String,
    detail: String,
    balance: String?,
    chipState: TenantChipState,
) = TenantRowUiState(
    tenantId = name,
    name = name,
    detail = UiText.Plain(detail),
    balance = balance?.let(UiText::Plain),
    chipState = chipState,
)

private val ownerChrome =
    GroupChromeUiState(
        canAddTenants = true,
        menuItems =
            listOf(
                GroupMenuItem.Members,
                GroupMenuItem.Settings,
                GroupMenuItem.TransferOwnership,
                GroupMenuItem.Archive,
            ),
    )

private val populated =
    GroupUiState.Content(
        groupName = "1st Street",
        balance = UiText.Plain("Owes $3,900.00"),
        tenantCounts = UiText.Plain("6 tenants · 2 late"),
        archived = UiText.Plain("Archived (3) · Owes $2,400.00"),
        tenants =
            listOf(
                tenant("Apt 1A", "Marcus Lee · Due Apr 1", "Owes $1,650.00", TenantChipState.Late),
                tenant("Apt 3B", "Dana Whitfield · Due Apr 1", "Owes $550.00", TenantChipState.Late),
                tenant("Apt 3A", "Rafael Cruz · Due Apr 8", "Owes $1,400.00", TenantChipState.Owes),
                tenant("Apt 2C", "Priya Shah · Due Apr 5", "Owes $300.00", TenantChipState.Owes),
                tenant("Apt 2A", "Lena Novak · Due Apr 15", "Credit $400.00", TenantChipState.PaidUp),
                tenant("Apt 1B", "Tom & Jess Ortega · Due Apr 1", null, TenantChipState.PaidUp),
            ),
        chrome = ownerChrome,
    )

private val viewOnly =
    populated.copy(
        chrome = GroupChromeUiState(isViewOnly = true, menuItems = listOf(GroupMenuItem.Members, GroupMenuItem.Leave)),
    )

private val offline =
    populated.copy(chrome = GroupChromeUiState(), offlineBanner = UiText.Plain("Offline · last updated 2h ago"))

private val archivedGroup =
    GroupUiState.Content(
        groupName = "Elm Street Triplex",
        balance = UiText.Plain("Owes $800.00"),
        tenantCounts = UiText.Plain("2 tenants · none late"),
        archived = null,
        tenants =
            listOf(
                tenant("Unit A", "Noor Aziz · Archived Feb 2", "Owes $800.00", TenantChipState.Owes),
                tenant("Unit B", "Eli Brandt · Archived Feb 2", null, TenantChipState.PaidUp),
            ),
        chrome = GroupChromeUiState(isArchived = true),
    )

private val empty = GroupUiState.Empty(groupName = "1st Street", archived = null, chrome = ownerChrome)
