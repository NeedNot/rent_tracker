package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.AvatarKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.InitialsAvatar
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListItem
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_edit_name
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AccountProfileCard(
    initials: String,
    displayName: String,
    email: String,
    onEditNameClicked: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RtListCard(items = listOf(Unit), modifier = modifier) {
        RtListItem(
            text = displayName,
            supportingText = email,
            leading = { InitialsAvatar(initials = initials, kind = AvatarKind.Owner) },
            trailing =
                onEditNameClicked?.let { onClick ->
                    {
                        RtButton(
                            text = stringResource(Res.string.account_edit_name),
                            onClick = onClick,
                            variant = ButtonVariant.Text,
                        )
                    }
                },
        )
    }
}
