package net.neednot.renttracker.screenshots

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.presentation.screens.MembersActions
import net.neednot.renttracker.feature.members.presentation.screens.MembersContent
import net.neednot.renttracker.feature.members.presentation.uistate.InviteRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowAction
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MembersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.RemoveMemberDialogUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE_QUALIFIERS)
class MembersScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedInLightTheme() = members("members-populated", populated)

    @Test
    fun populatedInDarkTheme() = members("members-populated", populated, isDark = true)

    @Test
    fun readOnlyInLightTheme() = members("members-read-only", readOnly)

    @Test
    fun offlineInLightTheme() = members("members-offline", offline)

    @Test
    fun errorInLightTheme() =
        members("members-error", MembersUiState.Error(UiText.Plain("Something went wrong on our side.")))

    @Test
    fun removeDialogInLightTheme() =
        composeRule.captureScreenWithDialog("members-remove-dialog", isDark = false) {
            MembersContent(uiState = removeDialog, actions = MembersActions())
        }

    private fun members(
        name: String,
        uiState: MembersUiState,
        isDark: Boolean = false,
    ) = composeRule.captureScreen(name, isDark) { MembersContent(uiState = uiState, actions = MembersActions()) }
}

private fun row(
    id: String,
    name: String,
    label: MemberPermissionLabel,
    action: MemberRowAction = MemberRowAction.OpenPermissions,
    isYou: Boolean = false,
): MemberRowUiState {
    val initials = name.split(" ", "_").joinToString("") { it.take(1) }.uppercase()
    val email = if (isYou) "you@example.com" else "${name.lowercase().replace(" ", ".")}@example.com"
    return MemberRowUiState(id, initials, name, email, isYou, label, action)
}

private val owner = row("m1", "Need_Not", MemberPermissionLabel.Owner, MemberRowAction.None, isYou = true)

private val invite = InviteRowUiState("i1", "alex.m@example.com", MemberPermissionLabel.Bookkeeper, "Mar 16", true)

private val populated =
    MembersUiState.Content(
        groupName = "1st Street",
        showsViewOnlyInfo = false,
        members =
            listOf(
                owner,
                row("m2", "Sam Rivera", MemberPermissionLabel.Manager),
                row("m3", "Maria Lopez", MemberPermissionLabel.Bookkeeper),
                row("m4", "Jen Park", MemberPermissionLabel.Viewer),
            ),
        invites = listOf(invite),
        canInvite = true,
        canTransferOwnership = false,
    )

private val readOnly =
    populated.copy(
        showsViewOnlyInfo = true,
        members =
            listOf(
                row("m2", "Sam Rivera", MemberPermissionLabel.Owner, MemberRowAction.None),
                row("m1", "Need_Not", MemberPermissionLabel.Viewer, MemberRowAction.None, isYou = true),
                row("m3", "Maria Lopez", MemberPermissionLabel.Bookkeeper, MemberRowAction.None),
                row("m4", "Jen Park", MemberPermissionLabel.Viewer, MemberRowAction.None),
            ),
        invites = listOf(invite.copy(canRevoke = false)),
        canInvite = false,
    )

private val offline =
    populated.copy(
        members = populated.members.map { it.copy(action = MemberRowAction.None) },
        invites = listOf(invite.copy(canRevoke = false)),
        canInvite = false,
        offlineBanner = UiText.Plain("Offline · last updated 2h ago"),
    )

private val removeDialog =
    populated.copy(removeDialog = RemoveMemberDialogUiState("m3", "Maria Lopez", "Maria"))
