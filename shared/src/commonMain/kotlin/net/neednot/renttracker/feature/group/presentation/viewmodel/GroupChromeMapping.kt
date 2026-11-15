package net.neednot.renttracker.feature.group.presentation.viewmodel

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.presentation.uistate.GroupChromeUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupDialogUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupMenuItem
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.group_archive_tenant_count
import net.neednot.renttracker.resources.group_archived_banner

internal fun GroupDetails.toChrome(
    overlay: GroupOverlay,
    timeZone: TimeZone,
): GroupChromeUiState =
    GroupChromeUiState(
        isViewOnly = access.isViewOnly,
        canAddTenants = access.canAddTenants(),
        menuItems = access.menuItems(),
        archivedBanner = archivedBanner(timeZone),
        dialog = overlay.dialog?.toUiState(this),
        isWorking = overlay.isWorking,
    )

private fun CallerAccess.canAddTenants(): Boolean = !groupArchived && permissions.tenants >= AccessLevel.AddEdit

private fun CallerAccess.menuItems(): List<GroupMenuItem> =
    when {
        groupArchived -> emptyList()
        isOwner -> listOf(GroupMenuItem.Members, GroupMenuItem.Settings, GroupMenuItem.Archive)
        else -> listOf(GroupMenuItem.Members, GroupMenuItem.Settings, GroupMenuItem.Leave)
    }

private fun GroupDetails.archivedBanner(timeZone: TimeZone): UiText? =
    archivedAt?.let { instant ->
        UiText.Resource(Res.string.group_archived_banner, listOf(formatDate(instant.toLocalDateTime(timeZone).date)))
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
