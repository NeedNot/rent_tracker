package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingBannerUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.pending_banner_review
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PendingBanner(
    banner: PendingBannerUiState,
    onReviewClicked: (clientId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (banner) {
        is PendingBannerUiState.Waiting -> {
            WaitingBanner(banner, modifier)
        }

        is PendingBannerUiState.Attention -> {
            AttentionBanner(
                banner,
                { onReviewClicked(banner.reviewClientId) },
                modifier,
            )
        }
    }
}

@Composable
private fun WaitingBanner(
    banner: PendingBannerUiState.Waiting,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = banner.title.asString(), style = MaterialTheme.typography.titleSmall)
                Text(text = banner.subtitle.asString(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AttentionBanner(
    banner: PendingBannerUiState.Attention,
    onReviewClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = banner.title.asString(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = onReviewClicked, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(
                    text = stringResource(Res.string.pending_banner_review),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
