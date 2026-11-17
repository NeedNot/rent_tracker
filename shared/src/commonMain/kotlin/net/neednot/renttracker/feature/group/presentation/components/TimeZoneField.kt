package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import net.neednot.renttracker.core.designsystem.components.PickerField
import net.neednot.renttracker.core.designsystem.components.RtMenu
import net.neednot.renttracker.core.designsystem.components.RtMenuItem
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.uistate.TimeZoneOptionUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_settings_time_zone
import net.neednot.renttracker.resources.group_settings_time_zone_hint
import net.neednot.renttracker.resources.ic_arrow_drop_down
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TimeZoneField(
    options: List<TimeZoneOptionUiState>,
    selectedId: String,
    onSelected: (zoneId: String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var fieldWidth by remember { mutableStateOf(Dp.Unspecified) }
    val density = LocalDensity.current
    Box(modifier = modifier.onSizeChanged { fieldWidth = with(density) { it.width.toDp() } }) {
        PickerField(
            label = stringResource(Res.string.group_settings_time_zone),
            value =
                options
                    .firstOrNull { it.id == selectedId }
                    ?.label
                    ?.asString()
                    .orEmpty(),
            onOpenClicked = { if (enabled) isOpen = true },
            hint = stringResource(Res.string.group_settings_time_zone_hint),
            enabled = enabled,
            icon = Res.drawable.ic_arrow_drop_down,
        )
        RtMenu(expanded = isOpen, onDismissed = { isOpen = false }, modifier = Modifier.width(fieldWidth)) {
            options.forEach { option ->
                RtMenuItem(
                    text = option.label.asString(),
                    onClick = {
                        isOpen = false
                        onSelected(option.id)
                    },
                )
            }
        }
    }
}
