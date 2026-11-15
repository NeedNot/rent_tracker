package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_menu_archive
import net.neednot.renttracker.resources.tenant_menu_edit_details
import net.neednot.renttracker.resources.tenant_menu_move
import net.neednot.renttracker.resources.tenant_menu_restore
import net.neednot.renttracker.resources.tenant_more_options
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TenantMenu(
    isArchived: Boolean,
    canMove: Boolean,
    onEditDetailsClicked: () -> Unit,
    onMoveClicked: () -> Unit,
    onArchiveClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { isOpen = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(Res.string.tenant_more_options),
        )
    }
    DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
        if (isArchived) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.tenant_menu_restore)) },
                onClick = {
                    isOpen = false
                    onRestoreClicked()
                },
            )
        } else {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.tenant_menu_edit_details)) },
                onClick = {
                    isOpen = false
                    onEditDetailsClicked()
                },
            )
            if (canMove) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.tenant_menu_move)) },
                    onClick = {
                        isOpen = false
                        onMoveClicked()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.tenant_menu_archive)) },
                onClick = {
                    isOpen = false
                    onArchiveClicked()
                },
            )
        }
    }
}
