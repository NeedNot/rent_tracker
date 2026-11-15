package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.viewmodel.label
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_settings_payment_order
import net.neednot.renttracker.resources.group_settings_payment_order_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaymentOrderToggle(
    selected: PaymentOrder,
    onSelected: (PaymentOrder) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(Res.string.group_settings_payment_order),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PaymentOrder.entries.forEachIndexed { index, order ->
                SegmentedButton(
                    selected = order == selected,
                    onClick = { onSelected(order) },
                    shape = SegmentedButtonDefaults.itemShape(index, PaymentOrder.entries.size),
                    enabled = enabled,
                ) {
                    Text(order.label().asString())
                }
            }
        }
        Text(
            text = stringResource(Res.string.group_settings_payment_order_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
