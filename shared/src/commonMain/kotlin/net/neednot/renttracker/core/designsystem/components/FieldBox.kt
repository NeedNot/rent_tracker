package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.designsystem.theme.Opacity
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing

@Composable
internal fun FieldBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelNote: String? = null,
    error: String? = null,
    hint: String? = null,
    counter: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    interactionSource: MutableInteractionSource? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().alpha(if (enabled) 1f else Opacity.DISABLED),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        FieldLabel(
            label = label,
            modifier = Modifier.clearAndSetSemantics {},
            note = labelNote,
            isError = error != null,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.field)
                    .semantics { contentDescription = label },
            enabled = enabled,
            readOnly = readOnly,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = placeholder?.let { { Text(it) } },
            prefix = prefix?.let { { Text(it) } },
            trailingIcon = trailingIcon,
            isError = error != null,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.small,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = scheme.surfaceContainerLow,
                    unfocusedContainerColor = scheme.surfaceContainerLow,
                    errorContainerColor = scheme.surfaceContainerLow,
                    disabledContainerColor = scheme.surfaceContainerLow,
                    unfocusedBorderColor = scheme.outline,
                    disabledBorderColor = scheme.outline,
                    disabledTextColor = scheme.onSurface,
                    disabledPrefixColor = scheme.onSurfaceVariant,
                    disabledTrailingIconColor = scheme.onSurfaceVariant,
                    disabledPlaceholderColor = scheme.onSurfaceVariant,
                ),
        )
        FieldFoot(error = error, hint = hint, counter = counter)
    }
}
