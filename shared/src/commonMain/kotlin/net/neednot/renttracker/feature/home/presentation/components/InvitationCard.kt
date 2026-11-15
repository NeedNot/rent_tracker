package net.neednot.renttracker.feature.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.uistate.InvitationCardUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_invitation_accept
import net.neednot.renttracker.resources.home_invitation_decline
import org.jetbrains.compose.resources.stringResource

@Composable
fun InvitationCard(
    invitation: InvitationCardUiState,
    onAcceptClicked: () -> Unit,
    onDeclineClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = invitation.description.asString()
    OutlinedCard(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MailBadge()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDeclineClicked, enabled = !invitation.isWorking) {
                    Text(stringResource(Res.string.home_invitation_decline))
                }
                Button(onClick = onAcceptClicked, enabled = !invitation.isWorking) {
                    Text(stringResource(Res.string.home_invitation_accept))
                }
            }
        }
    }
}

@Composable
private fun MailBadge() {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Icon(
            imageVector = Icons.Outlined.MailOutline,
            contentDescription = null,
            modifier = Modifier.padding(10.dp),
        )
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
