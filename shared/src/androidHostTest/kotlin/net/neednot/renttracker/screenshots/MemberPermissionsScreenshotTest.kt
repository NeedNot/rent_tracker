package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.presentation.screens.MemberPermissionsActions
import net.neednot.renttracker.feature.members.presentation.screens.MemberPermissionsContent
import net.neednot.renttracker.feature.members.presentation.uistate.MemberHeaderUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberPermissionsUiState
import net.neednot.renttracker.feature.members.presentation.viewmodel.permissionsForm
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class MemberPermissionsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = permissions("member-permissions", populated)

    @Test
    fun populatedInDarkTheme() = permissions("member-permissions", populated, isDark = true)

    @Test
    fun viewerInLightTheme() = permissions("member-permissions-viewer", viewer)

    @Test
    fun conflictInLightTheme() = permissions("member-permissions-conflict", conflict)

    @Test
    fun errorInLightTheme() =
        permissions(
            "member-permissions-error",
            MemberPermissionsUiState(isLoading = false, error = UiText.Plain("This member no longer exists.")),
        )

    private fun permissions(
        name: String,
        uiState: MemberPermissionsUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        MemberPermissionsContent(uiState = uiState, actions = MemberPermissionsActions())
    }
}

private val ownerAccess =
    CallerAccess(
        membershipId = "m1",
        accountId = "me",
        role = MembershipRole.Owner,
        permissions = PermissionPreset.ownerPermissions,
        groupArchived = false,
    )

private val kevinsPermissions =
    PermissionPreset.Bookkeeper.permissions.copy(members = AccessLevel.AddEdit, editOthers = true)

private val populated =
    MemberPermissionsUiState(
        isLoading = false,
        header = MemberHeaderUiState("KC", "Kevin Cho", "kevin.cho@example.com"),
        form = ownerAccess.permissionsForm(kevinsPermissions, saved = kevinsPermissions),
        canRemove = true,
    )

private val viewer =
    populated.copy(
        form = ownerAccess.permissionsForm(PermissionPreset.Viewer.permissions, saved = kevinsPermissions),
    )

private val conflict =
    populated.copy(
        conflictBanner = UiText.Plain("We reloaded Kevin's latest permissions. Check them, then save again."),
    )
