package net.neednot.renttracker.feature.debts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.charge_editor_kind_custom
import net.neednot.renttracker.resources.charge_editor_kind_late_fee
import net.neednot.renttracker.resources.charge_editor_type
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FixedChargeKind(
    kind: DebtKind,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.charge_editor_type),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        val label =
            when (kind) {
                DebtKind.LateFee -> Res.string.charge_editor_kind_late_fee
                DebtKind.Custom -> Res.string.charge_editor_kind_custom
            }
        Text(text = stringResource(label), style = MaterialTheme.typography.bodyLarge)
    }
}
