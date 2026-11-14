package net.neednot.renttracker.feature.debts.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.charge_editor_kind_custom
import net.neednot.renttracker.resources.charge_editor_kind_late_fee
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ChargeKindToggle(
    selected: DebtKind,
    onKindSelected: (DebtKind) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        DebtKind.entries.forEachIndexed { index, kind ->
            SegmentedButton(
                selected = kind == selected,
                onClick = { onKindSelected(kind) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = DebtKind.entries.size),
                enabled = enabled,
            ) {
                Text(stringResource(kind.label))
            }
        }
    }
}

private val DebtKind.label
    get() =
        when (this) {
            DebtKind.LateFee -> Res.string.charge_editor_kind_late_fee
            DebtKind.Custom -> Res.string.charge_editor_kind_custom
        }
