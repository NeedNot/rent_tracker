package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.AvatarKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.IconAvatar
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtListRow
import net.neednot.renttracker.feature.members.presentation.uistate.InviteRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_mail
import net.neednot.renttracker.resources.members_invite_detail
import net.neednot.renttracker.resources.members_revoke
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InviteRow(
    invite: InviteRowUiState,
    onRevokeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RtListRow(
        title = invite.email,
        modifier = modifier,
        subtitle =
            stringResource(
                Res.string.members_invite_detail,
                stringResource(invite.label.text),
                invite.expiresOn,
            ),
        leading = { IconAvatar(icon = Res.drawable.ic_mail, kind = AvatarKind.Invite) },
        trailing =
            if (invite.canRevoke) {
                {
                    RtButton(
                        text = stringResource(Res.string.members_revoke),
                        onClick = onRevokeClicked,
                        variant = ButtonVariant.Text,
                    )
                }
            } else {
                null
            },
    )
}
