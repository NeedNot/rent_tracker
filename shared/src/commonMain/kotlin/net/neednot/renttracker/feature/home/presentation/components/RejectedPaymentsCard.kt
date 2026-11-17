package net.neednot.renttracker.feature.home.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_rejected_payments_hint
import net.neednot.renttracker.resources.ic_sync_problem
import org.jetbrains.compose.resources.stringResource

@Composable
fun RejectedPaymentsCard(
    summary: UiText,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Banner(
        text = stringResource(Res.string.home_rejected_payments_hint),
        kind = BannerKind.Error,
        modifier = modifier,
        title = summary.asString(),
        icon = Res.drawable.ic_sync_problem,
        onClick = onClick,
    )
}

@Preview
@Composable
private fun RejectedPaymentsCardPreview() {
    RentTrackerTheme {
        RejectedPaymentsCard(summary = UiText.Plain("2 payments couldn't sync"), onClick = {})
    }
}
