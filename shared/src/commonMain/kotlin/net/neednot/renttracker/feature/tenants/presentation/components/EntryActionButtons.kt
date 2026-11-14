package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_add_charge
import net.neednot.renttracker.resources.ledger_log_payment
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryActionButtons(
    canLogPayment: Boolean,
    canAddCharge: Boolean,
    onLogPaymentClicked: () -> Unit,
    onAddChargeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!canLogPayment && !canAddCharge) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canLogPayment) {
            Button(onClick = onLogPaymentClicked, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = stringResource(Res.string.ledger_log_payment), modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (canAddCharge) {
            OutlinedButton(onClick = onAddChargeClicked, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.ledger_add_charge))
            }
        }
    }
}
