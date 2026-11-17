package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.FieldGroup
import net.neednot.renttracker.core.designsystem.components.RtCard
import net.neednot.renttracker.core.designsystem.components.RtFilterChip
import net.neednot.renttracker.core.designsystem.components.RtListItem
import net.neednot.renttracker.core.designsystem.components.RtSegmentedButton
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.feature.members.presentation.uistate.EditOthersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.LevelRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionsFormUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_categories
import net.neednot.renttracker.resources.members_edit_others
import net.neednot.renttracker.resources.members_edit_others_description
import net.neednot.renttracker.resources.members_edit_others_inactive
import net.neednot.renttracker.resources.members_label_custom
import net.neednot.renttracker.resources.members_preset
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PermissionEditor(
    form: PermissionsFormUiState,
    showsCategories: Boolean,
    actions: PermissionEditorActions,
    modifier: Modifier = Modifier,
    presetHint: String? = null,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ContentLayout.Form.spacing)) {
        FieldGroup(label = stringResource(Res.string.members_preset), hint = presetHint) {
            PresetChips(
                presets = form.presets,
                selected = form.selectedPreset,
                showsCustom = showsCategories || form.selectedPreset == null,
                onPresetClicked = actions.onPresetClicked,
                enabled = enabled,
            )
        }
        if (showsCategories) {
            CategoriesCard(
                levelRows = form.levelRows,
                editOthers = form.editOthers,
                onLevelSelected = actions.onLevelSelected,
                onEditOthersChanged = actions.onEditOthersChanged,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun PresetChips(
    presets: List<PermissionPreset>,
    selected: PermissionPreset?,
    showsCustom: Boolean,
    onPresetClicked: (PermissionPreset) -> Unit,
    enabled: Boolean,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        presets.forEach { preset ->
            RtFilterChip(
                text = stringResource(preset.text),
                selected = preset == selected,
                onClick = { onPresetClicked(preset) },
                enabled = enabled,
            )
        }
        if (showsCustom) {
            RtFilterChip(
                text = stringResource(Res.string.members_label_custom),
                selected = selected == null,
                onClick = UnimplementedAction,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun CategoriesCard(
    levelRows: List<LevelRowUiState>,
    editOthers: EditOthersUiState,
    onLevelSelected: (PermissionArea, AccessLevel) -> Unit,
    onEditOthersChanged: (Boolean) -> Unit,
    enabled: Boolean,
) {
    val description = stringResource(Res.string.members_categories)
    RtCard(modifier = Modifier.semantics { contentDescription = description }) {
        levelRows.forEach { row ->
            LevelRow(row = row, onLevelSelected = { onLevelSelected(row.area, it) }, enabled = enabled)
        }
        EditOthersRow(editOthers = editOthers, onChanged = onEditOthersChanged, enabled = enabled)
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
        modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (row.levels.size == 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = areaName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(row.selected.text),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Text(text = areaName, style = MaterialTheme.typography.titleMedium)
            RtSegmentedButton(
                options = row.levels.map { stringResource(it.text) },
                selectedIndex = row.levels.indexOf(row.selected),
                onSelected = { onLevelSelected(row.levels[it]) },
                modifier = Modifier.semantics { contentDescription = areaName },
                enabled = enabled,
            )
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
    RtListItem(
        text = title,
        supportingText = stringResource(description),
        trailing = {
            Switch(
                checked = editOthers.isOn,
                onCheckedChange = onChanged,
                modifier = Modifier.semantics { contentDescription = title },
                enabled = enabled && editOthers.isEnabled,
            )
        },
    )
}
