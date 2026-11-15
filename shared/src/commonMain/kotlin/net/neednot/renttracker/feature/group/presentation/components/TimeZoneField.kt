package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.uistate.TimeZoneOptionUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_settings_time_zone
import net.neednot.renttracker.resources.group_settings_time_zone_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimeZoneField(
    options: List<TimeZoneOptionUiState>,
    selectedId: String,
    onSelected: (zoneId: String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(Res.string.group_settings_time_zone)
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value =
                options
                    .firstOrNull { it.id == selectedId }
                    ?.label
                    ?.asString()
                    .orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null) },
            supportingText = { Text(stringResource(Res.string.group_settings_time_zone_hint)) },
            singleLine = true,
        )
        if (enabled) {
            Box(modifier = Modifier.matchParentSize().clickable(role = Role.DropdownList) { isOpen = true })
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label.asString()) },
                    onClick = {
                        isOpen = false
                        onSelected(option.id)
                    },
                )
            }
        }
    }
}
