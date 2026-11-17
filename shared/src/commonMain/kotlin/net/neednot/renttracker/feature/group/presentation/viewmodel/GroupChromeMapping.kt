package net.neednot.renttracker.feature.group.presentation.viewmodel

import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.presentation.uistate.GroupChromeUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupDialogUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupMenuItem
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.group_archive_tenant_count

internal fun GroupDetails.toChrome(
    overlay: GroupOverlay,
    freshness: Freshness,
): GroupChromeUiState {
    val isOnline = freshness == Freshness.Online
    return GroupChromeUiState(
        isViewOnly = access.isViewOnly,
        isArchived = archivedAt != null,
        canAddTenants = isOnline && access.canAddTenants(),
        canUnarchive = isOnline,
        menuItems = if (isOnline) access.menuItems() else emptyList(),
        dialog = overlay.dialog?.toUiState(this),
        isWorking = overlay.isWorking,
    )
}

private fun CallerAccess.canAddTenants(): Boolean = !groupArchived && permissions.tenants >= AccessLevel.AddEdit

private fun CallerAccess.menuItems(): List<GroupMenuItem> =
    when {
        groupArchived -> {
            emptyList()
        }

        isOwner -> {
            listOf(
                GroupMenuItem.Members,
                GroupMenuItem.Settings,
                GroupMenuItem.TransferOwnership,
                GroupMenuItem.Archive,
            )
        }

        else -> {
            listOf(GroupMenuItem.Members, GroupMenuItem.Settings, GroupMenuItem.Leave)
        }
    }

private fun GroupDialog.toUiState(group: GroupDetails): GroupDialogUiState =
    when (this) {
        GroupDialog.Archive -> {
            GroupDialogUiState.Archive(
                groupName = group.name,
                tenantCount =
                    UiText.Plural(
                        Res.plurals.group_archive_tenant_count,
                        group.activeTenants.size,
                        listOf(group.activeTenants.size),
                    ),
                balance =
                    if (group.totalOwedCents == 0L) {
                        UiText.Resource(Res.string.balance_paid_up)
                    } else {
                        owesText(group.totalOwedCents)
                    },
            )
        }

        GroupDialog.Leave -> {
            GroupDialogUiState.Leave(group.name)
        }

        GroupDialog.LeaveOwner -> {
            GroupDialogUiState.LeaveOwner(group.name)
        }
    }
