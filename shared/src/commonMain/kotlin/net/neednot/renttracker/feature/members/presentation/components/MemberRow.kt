package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.AvatarKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.InitialsAvatar
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtListRow
import net.neednot.renttracker.core.designsystem.components.Tag
import net.neednot.renttracker.core.designsystem.components.TagKind
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowAction
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_chevron_right
import net.neednot.renttracker.resources.members_remove
import net.neednot.renttracker.resources.members_you
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MemberRow(
    member: MemberRowUiState,
    onClick: () -> Unit,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RtListRow(
        title = member.name,
        modifier = modifier,
        titleNote = if (member.isYou) stringResource(Res.string.members_you) else null,
        subtitle = member.email,
        onClick = if (member.action == MemberRowAction.OpenPermissions) onClick else null,
        leading = {
            InitialsAvatar(
                initials = member.initials,
                kind = if (member.isOwner) AvatarKind.Owner else AvatarKind.Member,
            )
        },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MemberTag(label = member.label)
                MemberRowTrailingAction(action = member.action, onRemoveClicked = onRemoveClicked)
            }
        },
    )
}

@Composable
private fun MemberTag(label: MemberPermissionLabel) {
    Tag(
        text = stringResource(label.text),
        kind = if (label == MemberPermissionLabel.Owner) TagKind.Outlined else TagKind.Neutral,
    )
}

@Composable
private fun MemberRowTrailingAction(
    action: MemberRowAction,
    onRemoveClicked: () -> Unit,
) {
    when (action) {
        MemberRowAction.OpenPermissions -> {
            AppIcon(
                icon = Res.drawable.ic_chevron_right,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        MemberRowAction.Remove -> {
            RtButton(
                text = stringResource(Res.string.members_remove),
                onClick = onRemoveClicked,
                variant = ButtonVariant.Text,
                tone = ActionTone.Destructive,
            )
        }

        MemberRowAction.None -> {}
    }
}
