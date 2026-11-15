package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.feature.members.presentation.uistate.EditOthersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.LevelRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionsFormUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_edit_others
import net.neednot.renttracker.resources.members_edit_others_description
import net.neednot.renttracker.resources.members_edit_others_inactive
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PermissionLevelsCard(
    form: PermissionsFormUiState,
    onLevelSelected: (PermissionArea, AccessLevel) -> Unit,
    onEditOthersChanged: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        form.levelRows.forEach { row ->
            LevelRow(row = row, onLevelSelected = { onLevelSelected(row.area, it) }, enabled = enabled)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        }
        EditOthersRow(editOthers = form.editOthers, onChanged = onEditOthersChanged, enabled = enabled)
    }
}

@Composable
private fun LevelRow(
    row: LevelRowUiState,
    onLevelSelected: (AccessLevel) -> Unit,
    enabled: Boolean,
) {
    val areaName = stringResource(row.area.text)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (row.levels.size == 1) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = areaName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(text = stringResource(row.selected.text), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Text(text = areaName, style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = areaName },
            ) {
                row.levels.forEachIndexed { index, level ->
                    SegmentedButton(
                        selected = level == row.selected,
                        onClick = { onLevelSelected(level) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = row.levels.size),
                        enabled = enabled,
                    ) {
                        Text(text = stringResource(level.text), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditOthersRow(
    editOthers: EditOthersUiState,
    onChanged: (Boolean) -> Unit,
    enabled: Boolean,
) {
    val title = stringResource(Res.string.members_edit_others)
    val description =
        if (editOthers.appliesNow) {
            Res.string.members_edit_others_description
        } else {
            Res.string.members_edit_others_inactive
        }
    val titleColor =
        if (editOthers.appliesNow) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = titleColor, style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = editOthers.isOn,
            onCheckedChange = onChanged,
            modifier = Modifier.semantics { contentDescription = title },
            enabled = enabled && editOthers.isEnabled,
        )
    }
}
