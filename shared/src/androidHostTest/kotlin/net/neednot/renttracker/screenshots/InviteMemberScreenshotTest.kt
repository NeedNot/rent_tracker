package net.neednot.renttracker.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.presentation.screens.InviteMemberActions
import net.neednot.renttracker.feature.members.presentation.screens.InviteMemberContent
import net.neednot.renttracker.feature.members.presentation.uistate.InviteMemberUiState
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
class InviteMemberScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = invite("invite-member", populated)

    @Test
    fun populatedInDarkTheme() = invite("invite-member", populated, isDark = true)

    @Test
    fun customizedInLightTheme() = invite("invite-member-customized", customized)

    @Test
    fun customizedInDarkTheme() = invite("invite-member-customized", customized, isDark = true)

    @Test
    fun emailErrorInLightTheme() = invite("invite-member-error", emailError)

    @Test
    fun sendingInLightTheme() = invite("invite-member-sending", populated.copy(isSending = true))

    private fun invite(
        name: String,
        uiState: InviteMemberUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) {
        InviteMemberContent(uiState = uiState, actions = InviteMemberActions())
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

private val populated =
    InviteMemberUiState(
        isLoading = false,
        email = "alex.m@example.com",
        form = ownerAccess.permissionsForm(PermissionPreset.Bookkeeper.permissions),
    )

private val customized =
    populated.copy(
        form =
            ownerAccess.permissionsForm(
                PermissionPreset.Bookkeeper.permissions.copy(members = AccessLevel.AddEdit, editOthers = true),
            ),
        isCustomizeExpanded = true,
    )

private val emailError =
    populated.copy(
        email = "sam.rivera@example.com",
        emailError = UiText.Plain("Already a member of this group."),
    )
