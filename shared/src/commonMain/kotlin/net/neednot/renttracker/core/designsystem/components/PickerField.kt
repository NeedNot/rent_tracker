package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_choose_field
import net.neednot.renttracker.resources.common_clear_field
import net.neednot.renttracker.resources.ic_calendar_today
import net.neednot.renttracker.resources.ic_close
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PickerField(
    label: String,
    value: String,
    onOpenClicked: () -> Unit,
    modifier: Modifier = Modifier,
    labelNote: String? = null,
    error: UiText? = null,
    hint: String? = null,
    enabled: Boolean = true,
    icon: DrawableResource = Res.drawable.ic_calendar_today,
    onCleared: (() -> Unit)? = null,
) {
    FieldBox(
        label = label,
        value = value,
        onValueChange = {},
        modifier = modifier,
        labelNote = labelNote,
        error = error?.asString(),
        hint = hint,
        enabled = enabled,
        readOnly = true,
        interactionSource = pressesOpening(onOpenClicked),
        trailingIcon = {
            if (onCleared != null && value.isNotEmpty()) {
                IconButton(onClick = onCleared, enabled = enabled) {
                    AppIcon(
                        icon = Res.drawable.ic_close,
                        contentDescription = stringResource(Res.string.common_clear_field, label),
                    )
                }
            } else {
                IconButton(onClick = onOpenClicked, enabled = enabled) {
                    AppIcon(icon = icon, contentDescription = stringResource(Res.string.common_choose_field, label))
                }
            }
        },
    )
}

// A read-only text field swallows clicks, so a press on it is what opens the picker.
@Composable
private fun pressesOpening(onOpenClicked: () -> Unit): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    val open by rememberUpdatedState(onOpenClicked)
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) open()
        }
    }
    return interactionSource
}
