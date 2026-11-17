package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.designsystem.components.AvatarKind
import net.neednot.renttracker.core.designsystem.components.InitialsAvatar
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowAction
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowUiState
import net.neednot.renttracker.resources.Res
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
    val opensPermissions = member.action == MemberRowAction.OpenPermissions
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (opensPermissions) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(
            initials = member.initials,
            kind = if (member.isOwner) AvatarKind.Owner else AvatarKind.Member,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MemberName(name = member.name, isYou = member.isYou)
            member.email?.let { email ->
                Text(
                    text = email,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MemberLabelChip(member.label)
            when (member.action) {
                MemberRowAction.OpenPermissions -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                MemberRowAction.Remove -> {
                    TextButton(onClick = onRemoveClicked) {
                        Text(stringResource(Res.string.members_remove), color = MaterialTheme.colorScheme.error)
                    }
                }

                MemberRowAction.None -> {}
            }
        }
    }
}

@Composable
private fun MemberName(
    name: String,
    isYou: Boolean,
) {
    val youMarker = stringResource(Res.string.members_you)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text =
            buildAnnotatedString {
                append(name)
                if (isYou) {
                    append(" ")
                    withStyle(SpanStyle(color = mutedColor, fontWeight = FontWeight.Normal)) { append(youMarker) }
                }
            },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Preview
@Composable
private fun MemberRowPreview() {
    RentTrackerTheme {
        MemberRow(
            member =
                MemberRowUiState(
                    membershipId = "m2",
                    initials = "ML",
                    name = "Maria Lopez",
                    email = "maria.lopez@example.com",
                    isYou = false,
                    label = MemberPermissionLabel.Bookkeeper,
                    action = MemberRowAction.Remove,
                ),
            onClick = {},
            onRemoveClicked = {},
        )
    }
}
