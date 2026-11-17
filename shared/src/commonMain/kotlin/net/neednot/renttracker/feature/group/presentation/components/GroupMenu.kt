package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.RtMenu
import net.neednot.renttracker.core.designsystem.components.RtMenuDivider
import net.neednot.renttracker.core.designsystem.components.RtMenuItem
import net.neednot.renttracker.feature.group.presentation.uistate.GroupMenuItem
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_more_options
import net.neednot.renttracker.resources.group_menu_archive
import net.neednot.renttracker.resources.group_menu_leave
import net.neednot.renttracker.resources.group_menu_members
import net.neednot.renttracker.resources.group_menu_settings
import net.neednot.renttracker.resources.group_transfer_ownership
import net.neednot.renttracker.resources.ic_group
import net.neednot.renttracker.resources.ic_inventory_2
import net.neednot.renttracker.resources.ic_logout
import net.neednot.renttracker.resources.ic_more_vert
import net.neednot.renttracker.resources.ic_settings
import net.neednot.renttracker.resources.ic_swap_horiz
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupMenu(
    items: List<GroupMenuItem>,
    onItemClicked: (GroupMenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { isOpen = true }) {
            AppIcon(
                icon = Res.drawable.ic_more_vert,
                contentDescription = stringResource(Res.string.common_more_options),
            )
        }
        RtMenu(expanded = isOpen, onDismissed = { isOpen = false }) {
            items.forEach { item ->
                if (item.isDestructive()) RtMenuDivider()
                RtMenuItem(
                    text = stringResource(item.label()),
                    onClick = {
                        isOpen = false
                        onItemClicked(item)
                    },
                    icon = item.icon(),
                    tone = if (item.isDestructive()) ActionTone.Destructive else ActionTone.Default,
                )
            }
        }
    }
}

private fun GroupMenuItem.isDestructive(): Boolean = this == GroupMenuItem.Archive || this == GroupMenuItem.Leave

private fun GroupMenuItem.label(): StringResource =
    when (this) {
        GroupMenuItem.Members -> Res.string.group_menu_members
        GroupMenuItem.Settings -> Res.string.group_menu_settings
        GroupMenuItem.TransferOwnership -> Res.string.group_transfer_ownership
        GroupMenuItem.Archive -> Res.string.group_menu_archive
        GroupMenuItem.Leave -> Res.string.group_menu_leave
    }

private fun GroupMenuItem.icon(): DrawableResource =
    when (this) {
        GroupMenuItem.Members -> Res.drawable.ic_group
        GroupMenuItem.Settings -> Res.drawable.ic_settings
        GroupMenuItem.TransferOwnership -> Res.drawable.ic_swap_horiz
        GroupMenuItem.Archive -> Res.drawable.ic_inventory_2
        GroupMenuItem.Leave -> Res.drawable.ic_logout
    }
