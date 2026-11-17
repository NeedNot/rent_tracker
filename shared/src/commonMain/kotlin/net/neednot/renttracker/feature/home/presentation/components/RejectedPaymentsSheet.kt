package net.neednot.renttracker.feature.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.DetailLayout
import net.neednot.renttracker.core.designsystem.components.DetailRow
import net.neednot.renttracker.core.designsystem.components.RtBottomSheet
import net.neednot.renttracker.core.designsystem.components.SheetAction
import net.neednot.renttracker.core.designsystem.components.SheetActions
import net.neednot.renttracker.core.designsystem.components.SheetHeader
import net.neednot.renttracker.core.designsystem.components.Tag
import net.neednot.renttracker.core.designsystem.components.TagKind
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.uistate.RejectedPaymentUiState
import net.neednot.renttracker.feature.home.presentation.uistate.RejectedPaymentsUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_rejected_payment_date_received
import net.neednot.renttracker.resources.home_rejected_payment_dismiss
import net.neednot.renttracker.resources.home_rejected_payment_rejected
import net.neednot.renttracker.resources.home_rejected_payment_why
import net.neednot.renttracker.resources.home_rejected_payments_sheet_title
import net.neednot.renttracker.resources.ic_block
import net.neednot.renttracker.resources.pending_rejected_not_saved
import org.jetbrains.compose.resources.stringResource

@Composable
fun RejectedPaymentsSheet(
    rejectedPayments: RejectedPaymentsUiState,
    onPaymentDismissed: (clientId: String) -> Unit,
    onSheetDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RtBottomSheet(onDismissed = onSheetDismissed, modifier = modifier) {
        Text(
            text = stringResource(Res.string.home_rejected_payments_sheet_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        rejectedPayments.payments.forEach { payment ->
            RejectedPaymentBlock(payment = payment, onDismissClicked = { onPaymentDismissed(payment.clientId) })
        }
    }
}

@Composable
private fun RejectedPaymentBlock(
    payment: RejectedPaymentUiState,
    onDismissClicked: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SheetHeader(
            label = payment.title.asString(),
            amount = payment.amount,
            trailing = {
                Tag(text = stringResource(Res.string.home_rejected_payment_rejected), kind = TagKind.Error)
            },
        )
        Column {
            DetailRow(
                label = stringResource(Res.string.home_rejected_payment_date_received),
                value = payment.dateReceived,
                layout = DetailLayout.Inline,
            )
            DetailRow(
                label = stringResource(Res.string.home_rejected_payment_why),
                value = payment.reason.asString(),
                layout = DetailLayout.Inline,
            )
        }
        Text(
            text = stringResource(Res.string.pending_rejected_not_saved),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        SheetActions {
            SheetAction(
                text = stringResource(Res.string.home_rejected_payment_dismiss),
                icon = Res.drawable.ic_block,
                onClick = onDismissClicked,
                tone = ActionTone.Destructive,
            )
        }
    }
}
