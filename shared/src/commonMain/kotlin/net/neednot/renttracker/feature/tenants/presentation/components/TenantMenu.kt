package net.neednot.renttracker.feature.tenants.presentation.components

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
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_more_options
import net.neednot.renttracker.resources.ic_drive_file_move
import net.neednot.renttracker.resources.ic_edit
import net.neednot.renttracker.resources.ic_inventory_2
import net.neednot.renttracker.resources.ic_more_vert
import net.neednot.renttracker.resources.ic_restore
import net.neednot.renttracker.resources.tenant_menu_archive
import net.neednot.renttracker.resources.tenant_menu_edit_details
import net.neednot.renttracker.resources.tenant_menu_move
import net.neednot.renttracker.resources.tenant_menu_restore
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TenantMenu(
    isArchived: Boolean,
    canMove: Boolean,
    onEditDetailsClicked: () -> Unit,
    onMoveClicked: () -> Unit,
    onArchiveClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    val select: (() -> Unit) -> () -> Unit = { action ->
        {
            isOpen = false
            action()
        }
    }
    Box(modifier = modifier) {
        IconButton(onClick = { isOpen = true }) {
            AppIcon(
                icon = Res.drawable.ic_more_vert,
                contentDescription = stringResource(Res.string.common_more_options),
            )
        }
        RtMenu(expanded = isOpen, onDismissed = { isOpen = false }) {
            if (isArchived) {
                RtMenuItem(
                    text = stringResource(Res.string.tenant_menu_restore),
                    onClick = select(onRestoreClicked),
                    icon = Res.drawable.ic_restore,
                )
            } else {
                RtMenuItem(
                    text = stringResource(Res.string.tenant_menu_edit_details),
                    onClick = select(onEditDetailsClicked),
                    icon = Res.drawable.ic_edit,
                )
                if (canMove) {
                    RtMenuItem(
                        text = stringResource(Res.string.tenant_menu_move),
                        onClick = select(onMoveClicked),
                        icon = Res.drawable.ic_drive_file_move,
                    )
                }
                RtMenuDivider()
                RtMenuItem(
                    text = stringResource(Res.string.tenant_menu_archive),
                    onClick = select(onArchiveClicked),
                    icon = Res.drawable.ic_inventory_2,
                    tone = ActionTone.Destructive,
                )
            }
        }
    }
}
