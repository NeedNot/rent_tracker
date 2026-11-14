package net.neednot.renttracker.feature.payments.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_editor_method
import net.neednot.renttracker.resources.payment_editor_method_last_used
import net.neednot.renttracker.resources.payment_editor_method_optional
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaymentMethodChips(
    selected: PaymentMethod?,
    lastUsed: PaymentMethod?,
    onMethodClicked: (PaymentMethod) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val legend =
            if (lastUsed == null) {
                stringResource(Res.string.payment_editor_method_optional)
            } else {
                stringResource(Res.string.payment_editor_method_last_used, stringResource(lastUsed.label))
            }
        Text(
            text = "${stringResource(Res.string.payment_editor_method)} $legend",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethod.entries.forEach { method ->
                FilterChip(
                    selected = method == selected,
                    onClick = { onMethodClicked(method) },
                    label = { Text(stringResource(method.label)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = enabled,
                )
            }
        }
    }
}
