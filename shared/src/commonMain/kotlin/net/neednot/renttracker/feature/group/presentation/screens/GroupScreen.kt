package net.neednot.renttracker.feature.group.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.ArchivedRow
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EmptyState
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtFab
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtSearchBar
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.SearchBarSize
import net.neednot.renttracker.core.designsystem.components.StatusChip
import net.neednot.renttracker.core.designsystem.components.StatusChipKind
import net.neednot.renttracker.core.designsystem.components.TenantChipState
import net.neednot.renttracker.core.designsystem.components.TopBarNavigation
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.designsystem.theme.plus
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.components.GroupDialog
import net.neednot.renttracker.feature.group.presentation.components.GroupMenu
import net.neednot.renttracker.feature.group.presentation.components.GroupSummary
import net.neednot.renttracker.feature.group.presentation.components.TenantRow
import net.neednot.renttracker.feature.group.presentation.uistate.GroupChromeUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupDialogUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupMenuItem
import net.neednot.renttracker.feature.group.presentation.uistate.GroupUiState
import net.neednot.renttracker.feature.group.presentation.uistate.TenantRowUiState
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupEvent
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_add_tenant
import net.neednot.renttracker.resources.group_archived_banner
import net.neednot.renttracker.resources.group_archived_banner_title
import net.neednot.renttracker.resources.group_empty_body
import net.neednot.renttracker.resources.group_empty_title
import net.neednot.renttracker.resources.group_error_title
import net.neednot.renttracker.resources.group_search_placeholder
import net.neednot.renttracker.resources.group_unarchive
import net.neednot.renttracker.resources.group_view_only
import net.neednot.renttracker.resources.ic_add
import net.neednot.renttracker.resources.ic_inventory_2
import net.neednot.renttracker.resources.ic_person_add
import net.neednot.renttracker.resources.ic_visibility
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupScreen(
    groupId: String,
    onBackClicked: () -> Unit,
    onTenantClicked: (tenantId: String) -> Unit,
    onAddTenantClicked: () -> Unit,
    onArchivedClicked: () -> Unit,
    onMembersClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onTransferOwnershipClicked: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = koinViewModel { parametersOf(groupId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GroupEvent.ReturnHome -> onReturnHome()
                GroupEvent.OpenTransferOwnership -> onTransferOwnershipClicked()
            }
        }
    }
    GroupContent(
        uiState = uiState,
        actions =
            GroupActions(
                onBackClicked = onBackClicked,
                onTenantClicked = onTenantClicked,
                onAddTenantClicked = onAddTenantClicked,
                onArchivedClicked = onArchivedClicked,
                onMenuItemClicked = { item ->
                    when (item) {
                        GroupMenuItem.Members -> onMembersClicked()
                        GroupMenuItem.Settings -> onSettingsClicked()
                        GroupMenuItem.TransferOwnership -> viewModel.onTransferOwnershipClicked()
                        GroupMenuItem.Archive -> viewModel.onArchiveClicked()
                        GroupMenuItem.Leave -> viewModel.onLeaveClicked()
                    }
                },
                onUnarchiveClicked = viewModel::onUnarchiveClicked,
                onDialogConfirmed = { dialog ->
                    when (dialog) {
                        is GroupDialogUiState.Archive -> viewModel.onArchiveConfirmed()
                        is GroupDialogUiState.Leave -> viewModel.onLeaveConfirmed()
                        is GroupDialogUiState.LeaveOwner -> viewModel.onTransferOwnershipClicked()
                    }
                },
                onDialogDismissed = viewModel::onDialogDismissed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun GroupContent(
    uiState: GroupUiState,
    actions: GroupActions,
    modifier: Modifier = Modifier,
) {
    val chrome = uiState.chrome()
    Scaffold(
        modifier = modifier,
        topBar = {
            RtTopAppBar(
                title = uiState.groupName(),
                onNavigationClicked = actions.onBackClicked,
                actions = {
                    if (chrome?.isViewOnly == true) {
                        StatusChip(
                            text = stringResource(Res.string.group_view_only),
                            kind = StatusChipKind.Neutral,
                            modifier = Modifier.padding(end = Spacing.xs),
                            icon = Res.drawable.ic_visibility,
                        )
                    }
                    if (chrome != null && chrome.menuItems.isNotEmpty()) {
                        GroupMenu(items = chrome.menuItems, onItemClicked = actions.onMenuItemClicked)
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState is GroupUiState.Content && uiState.chrome.canAddTenants) {
                RtFab(
                    text = stringResource(Res.string.group_add_tenant),
                    icon = Res.drawable.ic_add,
                    onClick = actions.onAddTenantClicked,
                )
            }
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            uiState.offlineBanner?.let { OfflineBanner(text = it.asString()) }
            GroupList(uiState = uiState, actions = actions)
        }
    }
    chrome?.dialog?.let { dialog ->
        GroupDialog(
            dialog = dialog,
            isWorking = chrome.isWorking,
            onConfirmed = { actions.onDialogConfirmed(dialog) },
            onDismissed = actions.onDialogDismissed,
        )
    }
}

@Composable
private fun GroupList(
    uiState: GroupUiState,
    actions: GroupActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ContentLayout.List.padding + PaddingValues(bottom = Sizes.fabClearance),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
    ) {
        when (uiState) {
            is GroupUiState.Loading -> {
                item(key = "loading") { LoadingIndicator(Modifier.fillParentMaxHeight(LOADING_HEIGHT_FRACTION)) }
            }

            is GroupUiState.Error -> {
                item(key = "error") {
                    ErrorState(
                        title = stringResource(Res.string.group_error_title),
                        text = uiState.message.asString(),
                        onRetryClicked = UnimplementedAction,
                    )
                }
            }

            is GroupUiState.Empty -> {
                archivedGroupItems(uiState.chrome, actions)
                archivedTenantsItem(uiState.archived, actions)
                item(key = "empty") { NoTenants(canAddTenants = uiState.chrome.canAddTenants, actions = actions) }
            }

            is GroupUiState.Content -> {
                tenantItems(uiState, actions)
            }
        }
    }
}

private const val LOADING_HEIGHT_FRACTION = 0.6f

private fun LazyListScope.tenantItems(
    content: GroupUiState.Content,
    actions: GroupActions,
) {
    content.syncError?.let { error ->
        item(key = "syncError") { Banner(text = error.asString(), kind = BannerKind.Error) }
    }
    archivedGroupItems(content.chrome, actions)
    if (!content.chrome.isArchived) {
        item(key = "summary") {
            GroupSummary(balance = content.balance.asString(), tenantCounts = content.tenantCounts.asString())
        }
        item(key = "search") {
            RtSearchBar(
                placeholder = stringResource(Res.string.group_search_placeholder),
                onClick = UnimplementedAction,
                size = SearchBarSize.Compact,
            )
        }
    }
    archivedTenantsItem(content.archived, actions)
    item(key = "tenants") {
        RtListCard(items = content.tenants) { tenant ->
            TenantRow(
                tenant = tenant,
                isArchived = content.chrome.isArchived,
                onClick = { actions.onTenantClicked(tenant.tenantId) },
            )
        }
    }
}

private fun LazyListScope.archivedGroupItems(
    chrome: GroupChromeUiState,
    actions: GroupActions,
) {
    if (!chrome.isArchived) return
    item(key = "archivedBanner") {
        Banner(
            title = stringResource(Res.string.group_archived_banner_title),
            text = stringResource(Res.string.group_archived_banner),
            kind = BannerKind.Info,
            icon = Res.drawable.ic_inventory_2,
        )
    }
    if (chrome.canUnarchive) {
        item(key = "unarchive") {
            RtButton(
                text = stringResource(Res.string.group_unarchive),
                onClick = actions.onUnarchiveClicked,
                modifier = Modifier.fillMaxWidth(),
                isWorking = chrome.isWorking,
            )
        }
    }
}

private fun LazyListScope.archivedTenantsItem(
    archived: UiText?,
    actions: GroupActions,
) {
    archived?.let {
        item(key = "archivedTenants") { ArchivedRow(text = it.asString(), onClick = actions.onArchivedClicked) }
    }
}

@Composable
private fun NoTenants(
    canAddTenants: Boolean,
    actions: GroupActions,
) {
    EmptyState(
        icon = Res.drawable.ic_person_add,
        title = stringResource(Res.string.group_empty_title),
        text = if (canAddTenants) stringResource(Res.string.group_empty_body) else null,
        action =
            if (canAddTenants) {
                {
                    RtButton(
                        text = stringResource(Res.string.group_add_tenant),
                        onClick = actions.onAddTenantClicked,
                        variant = ButtonVariant.Outlined,
                    )
                }
            } else {
                null
            },
    )
}

private fun GroupUiState.chrome(): GroupChromeUiState? =
    when (this) {
        is GroupUiState.Content -> chrome
        is GroupUiState.Empty -> chrome
        is GroupUiState.Loading, is GroupUiState.Error -> null
    }

private fun GroupUiState.groupName(): String =
    when (this) {
        is GroupUiState.Content -> groupName
        is GroupUiState.Empty -> groupName
        is GroupUiState.Loading, is GroupUiState.Error -> ""
    }

@Preview
@Composable
private fun GroupContentPreview() {
    RentTrackerTheme {
        GroupContent(
            uiState =
                GroupUiState.Content(
                    groupName = "1st Street",
                    balance = UiText.Plain("Owes $3,900.00"),
                    tenantCounts = UiText.Plain("6 tenants · 2 late"),
                    archived = UiText.Plain("Archived (3) · Owes $2,400.00"),
                    tenants =
                        listOf(
                            TenantRowUiState(
                                tenantId = "t1",
                                name = "Apt 1A",
                                detail = UiText.Plain("Marcus Lee · Due Apr 1"),
                                balance = UiText.Plain("Owes $1,650.00"),
                                chipState = TenantChipState.Late,
                            ),
                        ),
                    chrome = GroupChromeUiState(canAddTenants = true, menuItems = listOf(GroupMenuItem.Members)),
                ),
            actions = GroupActions(),
        )
    }
}
