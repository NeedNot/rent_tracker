package net.neednot.renttracker.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
internal fun FieldLabel(
    label: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    isError: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text =
            buildAnnotatedString {
                append(label)
                note?.let { withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append(" $it") } }
            },
        modifier = modifier,
        color = if (isError) scheme.error else scheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
    )
}
