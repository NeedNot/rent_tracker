package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import net.neednot.renttracker.feature.group.presentation.uistate.GroupMenuItem
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_menu_archive
import net.neednot.renttracker.resources.group_menu_leave
import net.neednot.renttracker.resources.group_menu_members
import net.neednot.renttracker.resources.group_menu_settings
import net.neednot.renttracker.resources.group_more_options
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GroupMenu(
    items: List<GroupMenuItem>,
    onItemClicked: (GroupMenuItem) -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { isOpen = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(Res.string.group_more_options),
        )
    }
    DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
        items.forEach { item ->
            if (item.isDestructiveGroupAction()) HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(item.label()),
                        color = if (item == GroupMenuItem.Leave) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                },
                onClick = {
                    isOpen = false
                    onItemClicked(item)
                },
            )
        }
    }
}

private fun GroupMenuItem.isDestructiveGroupAction(): Boolean =
    this == GroupMenuItem.Archive || this == GroupMenuItem.Leave

private fun GroupMenuItem.label(): StringResource =
    when (this) {
        GroupMenuItem.Members -> Res.string.group_menu_members
        GroupMenuItem.Settings -> Res.string.group_menu_settings
        GroupMenuItem.Archive -> Res.string.group_menu_archive
        GroupMenuItem.Leave -> Res.string.group_menu_leave
    }
