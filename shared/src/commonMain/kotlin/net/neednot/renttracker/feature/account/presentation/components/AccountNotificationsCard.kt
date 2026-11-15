package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.feature.account.presentation.uistate.GroupNotificationsRowUiState
import net.neednot.renttracker.feature.account.presentation.uistate.NotificationsSectionUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_notifications_blocked
import net.neednot.renttracker.resources.account_notifications_empty
import net.neednot.renttracker.resources.account_notifications_hint
import net.neednot.renttracker.resources.account_notifications_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AccountNotificationsCard(
    section: NotificationsSectionUiState.Shown,
    onGroupToggled: (groupId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(Res.string.account_notifications_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
            )
            SupportingText(stringResource(Res.string.account_notifications_hint))
            if (section.isBlockedOnPhone) {
                Text(
                    text = stringResource(Res.string.account_notifications_blocked),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (section.groups.isEmpty()) {
            SupportingText(
                text = stringResource(Res.string.account_notifications_empty),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        section.groups.forEach { group ->
            GroupNotificationsRow(group = group, onToggled = { onGroupToggled(group.groupId) })
        }
    }
}

@Composable
private fun GroupNotificationsRow(
    group: GroupNotificationsRowUiState,
    onToggled: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = group.isOn, role = Role.Switch, onValueChange = { onToggled() })
                .semantics { contentDescription = group.groupName }
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = group.groupName,
            modifier = Modifier.weight(1f).clearAndSetSemantics {},
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = group.isOn, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
private fun SupportingText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}
