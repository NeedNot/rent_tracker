package net.neednot.renttracker.feature.home.presentation.components

import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.AvatarKind
import net.neednot.renttracker.core.designsystem.components.AvatarSize
import net.neednot.renttracker.core.designsystem.components.InitialsAvatar
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_account
import net.neednot.renttracker.resources.ic_account_circle
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AccountButton(
    initial: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.home_account)
    IconButton(onClick = onClick, modifier = modifier.semantics { contentDescription = description }) {
        if (initial == null) {
            AppIcon(
                icon = Res.drawable.ic_account_circle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            InitialsAvatar(initials = initial, kind = AvatarKind.Owner, size = AvatarSize.Small)
        }
    }
}
