package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonSize
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.CalendarStateChip
import net.neednot.renttracker.core.designsystem.components.DetailLayout
import net.neednot.renttracker.core.designsystem.components.DetailRow
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.components.RowAmount
import net.neednot.renttracker.core.designsystem.components.RtBottomSheet
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListItem
import net.neednot.renttracker.core.designsystem.components.SectionHeader
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.AddFeeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthDetailUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthLateFeeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthLateNoticeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthPaymentUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_add_fee
import net.neednot.renttracker.resources.calendar_covered_by
import net.neednot.renttracker.resources.ic_receipt_long
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MonthDetailHost(
    detail: MonthDetailUiState,
    actions: CalendarActions,
) {
    RtBottomSheet(onDismissed = actions.onMonthDetailDismissed) {
        MonthDetailSheet(detail = detail, onAddFeeClicked = actions.onAddFeeClicked)
    }
}

@Composable
internal fun MonthDetailSheet(
    detail: MonthDetailUiState,
    onAddFeeClicked: (AddFeeUiState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        detail.offlineBanner?.let { OfflineBanner(it.asString()) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = detail.title,
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            CalendarStateChip(state = detail.state, label = detail.stateLabel.asString())
        }
        DetailRow(label = detail.rentLabel.asString(), value = detail.rent, layout = DetailLayout.Inline)
        detail.coverageNote?.let { note ->
            Text(
                text = note.asString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (detail.payments.isNotEmpty()) CoveredBy(detail.payments)
        if (detail.totals.isNotEmpty() || detail.lateFee != null) {
            Column {
                detail.totals.forEach { line ->
                    DetailRow(label = line.label.asString(), value = line.amount, layout = DetailLayout.Inline)
                }
                detail.lateFee?.let { fee ->
                    DetailRow(label = fee.title.asString(), value = fee.amount, layout = DetailLayout.Inline)
                }
            }
        }
        detail.lateFee?.let { fee -> LateFeeNotes(fee) }
        detail.lateNotice?.let { notice -> LateNotice(notice, onAddFeeClicked) }
    }
}

@Composable
private fun CoveredBy(payments: List<MonthPaymentUiState>) {
    SectionHeader(text = stringResource(Res.string.calendar_covered_by))
    RtListCard(items = payments) { payment ->
        RtListItem(
            text = payment.title.asString(),
            supportingText =
                payment.notes
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.asString() }
                    ?.joinToString(" "),
            trailing = { RowAmount(payment.amount) },
        )
    }
}

@Composable
private fun LateFeeNotes(fee: MonthLateFeeUiState) {
    if (fee.notes.isEmpty()) return
    Banner(
        text = fee.notes.map { it.asString() }.joinToString(" "),
        kind = if (fee.isUnpaid) BannerKind.Warning else BannerKind.Info,
        icon = Res.drawable.ic_receipt_long,
    )
}

@Composable
private fun LateNotice(
    notice: MonthLateNoticeUiState,
    onAddFeeClicked: (AddFeeUiState) -> Unit,
) {
    Banner(
        text = notice.message.asString(),
        kind = BannerKind.Error,
        title = notice.title.asString(),
        action =
            notice.addFee?.let { addFee ->
                {
                    RtButton(
                        text = stringResource(Res.string.calendar_add_fee),
                        onClick = { onAddFeeClicked(addFee) },
                        variant = ButtonVariant.Outlined,
                        size = ButtonSize.Small,
                    )
                }
            },
    )
}
