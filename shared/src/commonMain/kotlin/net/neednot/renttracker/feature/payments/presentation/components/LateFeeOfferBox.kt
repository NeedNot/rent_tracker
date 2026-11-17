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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.presentation.uistate.LateFeeOfferUiState
import net.neednot.renttracker.feature.payments.presentation.viewmodel.monthName
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_editor_late_fee_amount
import net.neednot.renttracker.resources.payment_editor_late_fee_hint
import net.neednot.renttracker.resources.payment_editor_late_fee_offer
import net.neednot.renttracker.resources.payment_editor_late_fee_suggested
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LateFeeOfferBox(
    offer: LateFeeOfferUiState,
    amountError: UiText?,
    onToggled: (Boolean) -> Unit,
    onAmountChanged: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLow,
        border = BorderStroke(Sizes.strokeThin, if (offer.isChecked) scheme.primary else scheme.outlineVariant),
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = Spacing.lg,
                    top = Spacing.xs,
                    end = Spacing.lg,
                    bottom = if (offer.isChecked) Spacing.lg else Spacing.xs,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OfferHead(offer = offer, onToggled = onToggled, enabled = enabled)
            if (offer.isChecked) {
                MoneyField(
                    label = stringResource(Res.string.payment_editor_late_fee_amount),
                    text = offer.amount,
                    onTextChanged = onAmountChanged,
                    modifier = Modifier.padding(start = Spacing.xxxl),
                    error = amountError,
                    hint = stringResource(Res.string.payment_editor_late_fee_hint),
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun OfferHead(
    offer: LateFeeOfferUiState,
    onToggled: (Boolean) -> Unit,
    enabled: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.field)
                .toggleable(
                    value = offer.isChecked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onToggled,
                ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = offer.isChecked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = scheme.primary, uncheckedColor = scheme.onSurfaceVariant),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(Res.string.payment_editor_late_fee_offer, offer.month.monthName()),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!offer.isChecked) {
                Text(
                    text = stringResource(Res.string.payment_editor_late_fee_suggested, offer.suggestedAmount),
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
