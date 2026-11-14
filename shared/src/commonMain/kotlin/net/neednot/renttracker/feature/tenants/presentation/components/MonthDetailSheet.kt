package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.CalendarStateChip
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.AddFeeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.BalanceLineUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthDetailUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthLateFeeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthLateNoticeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthPaymentUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_add_fee
import net.neednot.renttracker.resources.calendar_covered_by
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonthDetailSheet(
    detail: MonthDetailUiState,
    onAddFeeClicked: (AddFeeUiState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = detail.title, style = MaterialTheme.typography.titleLarge)
            CalendarStateChip(state = detail.state, label = detail.stateLabel.asString())
        }
        AmountRow(label = detail.rentLabel.asString(), amount = detail.rent, isEmphasised = true)
        detail.coverageNote?.let { Text(text = it.asString(), style = MaterialTheme.typography.bodyMedium) }
        if (detail.payments.isNotEmpty()) CoveredBy(detail.payments)
        if (detail.totals.isNotEmpty()) Totals(detail.totals)
        detail.lateNotice?.let { notice -> LateNotice(notice, onAddFeeClicked) }
        detail.lateFee?.let { fee -> LateFeeBox(fee) }
    }
}

@Composable
private fun CoveredBy(payments: List<MonthPaymentUiState>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.calendar_covered_by),
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
        )
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            payments.forEachIndexed { index, payment ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PaymentRow(payment)
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: MonthPaymentUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = payment.title.asString(), style = MaterialTheme.typography.bodyMedium)
            payment.notes.forEach { note ->
                Text(
                    text = note.asString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text(text = payment.amount, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Totals(lines: List<BalanceLineUiState>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        lines.forEach { line -> AmountRow(label = line.label.asString(), amount = line.amount, isEmphasised = false) }
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: String,
    isEmphasised: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = amount,
            style = if (isEmphasised) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LateNotice(
    notice: MonthLateNoticeUiState,
    onAddFeeClicked: (AddFeeUiState) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = notice.title.asString(), style = MaterialTheme.typography.titleSmall)
                Text(text = notice.message.asString(), style = MaterialTheme.typography.bodySmall)
            }
            notice.addFee?.let { addFee ->
                Button(
                    onClick = { onAddFeeClicked(addFee) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(stringResource(Res.string.calendar_add_fee))
                }
            }
        }
    }
}

@Composable
private fun LateFeeBox(fee: MonthLateFeeUiState) {
    val accent = if (fee.isUnpaid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, accent),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = fee.title.asString(),
                    color = if (fee.isUnpaid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = fee.notes.map { it.asString() }.joinToString(" "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(text = fee.amount, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonthDetailHost(
    detail: MonthDetailUiState,
    actions: CalendarActions,
) {
    ModalBottomSheet(onDismissRequest = actions.onMonthDetailDismissed) {
        MonthDetailSheet(detail = detail, onAddFeeClicked = actions.onAddFeeClicked)
    }
}
