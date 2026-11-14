package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PickerField(
    label: String,
    value: String,
    onOpenClicked: () -> Unit,
    modifier: Modifier = Modifier,
    error: UiText? = null,
    hint: String? = null,
    enabled: Boolean = true,
    onCleared: (() -> Unit)? = null,
) {
    val supportingText = error?.asString() ?: hint
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            if (onCleared != null && value.isNotEmpty()) {
                IconButton(onClick = onCleared, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(Res.string.common_clear_field, label),
                    )
                }
            } else {
                IconButton(onClick = onOpenClicked, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = stringResource(Res.string.common_choose_field, label),
                    )
                }
            }
        },
        supportingText = supportingText?.let { { Text(it) } },
        isError = error != null,
        singleLine = true,
        interactionSource = pressesOpening(onOpenClicked),
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
