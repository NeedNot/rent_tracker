package net.neednot.renttracker.feature.payments.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.presentation.uistate.LateFeeOfferUiState
import net.neednot.renttracker.feature.payments.presentation.viewmodel.monthName
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_editor_late_fee_amount
import net.neednot.renttracker.resources.payment_editor_late_fee_hint
import net.neednot.renttracker.resources.payment_editor_late_fee_offer
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LateFeeOfferBox(
    offer: LateFeeOfferUiState,
    today: LocalDate,
    amountError: UiText?,
    onToggled: (Boolean) -> Unit,
    onAmountChanged: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = offer.isChecked,
                            enabled = enabled,
                            role = Role.Checkbox,
                            onValueChange = onToggled,
                        ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = offer.isChecked, onCheckedChange = null, enabled = enabled)
                Text(
                    text = stringResource(Res.string.payment_editor_late_fee_offer, offer.month.monthName()),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (offer.isChecked) {
                MoneyField(
                    label = stringResource(Res.string.payment_editor_late_fee_amount),
                    text = offer.amount,
                    onTextChanged = onAmountChanged,
                    error = amountError,
                    hint = stringResource(Res.string.payment_editor_late_fee_hint, formatDate(today)),
                    enabled = enabled,
                )
            }
        }
    }
}
