package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.InitialsAvatar
import net.neednot.renttracker.feature.members.presentation.uistate.TransferCandidateUiState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransferCandidateRow(
    candidate: TransferCandidateUiState,
    isSelected: Boolean,
    onSelected: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(selected = isSelected, onClick = onSelected, enabled = enabled, role = Role.RadioButton)
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = isSelected, onClick = null, enabled = enabled)
        InitialsAvatar(initials = candidate.initials)
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = candidate.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(candidate.label.text),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
