package net.neednot.renttracker.feature.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.ButtonSize
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.IconAvatar
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtCard
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.uistate.InvitationCardUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_invitation_accept
import net.neednot.renttracker.resources.home_invitation_decline
import net.neednot.renttracker.resources.ic_mail
import org.jetbrains.compose.resources.stringResource

@Composable
fun InvitationCard(
    invitation: InvitationCardUiState,
    onAcceptClicked: () -> Unit,
    onDeclineClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = invitation.description.asString()
    RtCard(
        modifier = modifier.semantics { contentDescription = description },
        borderColor = MaterialTheme.colorScheme.outlineVariant,
        contentPadding = PaddingValues(Spacing.lg),
        contentSpacing = Spacing.md,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconAvatar(icon = Res.drawable.ic_mail)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = invitation.groupName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = invitation.summary.asString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RtButton(
                text = stringResource(Res.string.home_invitation_decline),
                onClick = onDeclineClicked,
                variant = ButtonVariant.Text,
                enabled = !invitation.isWorking,
            )
            RtButton(
                text = stringResource(Res.string.home_invitation_accept),
                onClick = onAcceptClicked,
                size = ButtonSize.Small,
                enabled = !invitation.isWorking,
            )
        }
    }
}

@Preview
@Composable
private fun InvitationCardPreview() {
    RentTrackerTheme {
        InvitationCard(
            invitation =
                InvitationCardUiState(
                    inviteId = "i1",
                    groupName = "Oak Ave Duplex",
                    summary = UiText.Plain("Sam Rivera invited you as Bookkeeper"),
                    description = UiText.Plain("Invitation to Oak Ave Duplex"),
                    isWorking = false,
                ),
            onAcceptClicked = {},
            onDeclineClicked = {},
        )
    }
}
