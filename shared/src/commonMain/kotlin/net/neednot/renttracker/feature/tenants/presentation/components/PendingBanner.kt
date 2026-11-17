package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingBannerUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_sync
import net.neednot.renttracker.resources.ic_sync_problem

@Composable
internal fun PendingBanner(
    banner: PendingBannerUiState,
    onReviewClicked: (clientId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (banner) {
        is PendingBannerUiState.Waiting -> {
            Banner(
                text = banner.title.asString(),
                kind = BannerKind.Info,
                modifier = modifier,
                icon = Res.drawable.ic_sync,
            )
        }

        is PendingBannerUiState.Attention -> {
            Banner(
                text = banner.message.asString(),
                kind = BannerKind.Warning,
                modifier = modifier,
                title = banner.title.asString(),
                icon = Res.drawable.ic_sync_problem,
                onClick = { onReviewClicked(banner.reviewClientId) },
            )
        }
    }
}
