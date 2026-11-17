package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.theme.Spacing

@Composable
internal fun FieldGroup(
    label: String,
    modifier: Modifier = Modifier,
    labelNote: String? = null,
    error: String? = null,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FieldLabel(label = label, note = labelNote, isError = error != null)
        content()
        FieldFoot(error = error, hint = hint)
    }
}
