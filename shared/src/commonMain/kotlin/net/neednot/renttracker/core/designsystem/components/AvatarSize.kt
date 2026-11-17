package net.neednot.renttracker.core.designsystem.components

import androidx.compose.ui.unit.Dp
import net.neednot.renttracker.core.designsystem.theme.Sizes

enum class AvatarSize(
    val dp: Dp,
) {
    Regular(Sizes.avatar),
    Small(Sizes.avatarSmall),
}
