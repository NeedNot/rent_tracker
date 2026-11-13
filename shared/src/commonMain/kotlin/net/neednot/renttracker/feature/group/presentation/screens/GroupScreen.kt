package net.neednot.renttracker.feature.group.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.TenantChipState
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.components.ArchivedTenantsLink
import net.neednot.renttracker.feature.group.presentation.components.CenteredMessage
import net.neednot.renttracker.feature.group.presentation.components.LoadingIndicator
import net.neednot.renttracker.feature.group.presentation.components.SyncErrorText
import net.neednot.renttracker.feature.group.presentation.components.TenantListCard
import net.neednot.renttracker.feature.group.presentation.components.TenantRow
import net.neednot.renttracker.feature.group.presentation.uistate.GroupUiState
import net.neednot.renttracker.feature.group.presentation.uistate.TenantRowUiState
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupEvent
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_add_tenant
import net.neednot.renttracker.resources.group_back
import net.neednot.renttracker.resources.group_empty_body
import net.neednot.renttracker.resources.group_empty_title
import net.neednot.renttracker.resources.group_loading
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
    onGroupGone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = koinViewModel { parametersOf(groupId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GroupEvent.GroupGone -> onGroupGone()
            }
        }
    }
    GroupContent(
        uiState = uiState,
        onBackClicked = onBackClicked,
        onTenantClicked = onTenantClicked,
        onAddTenantClicked = onAddTenantClicked,
        onArchivedClicked = onArchivedClicked,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupContent(
    uiState: GroupUiState,
    onBackClicked: () -> Unit,
    onTenantClicked: (tenantId: String) -> Unit,
    onAddTenantClicked: () -> Unit,
    onArchivedClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.groupName()) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.group_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState is GroupUiState.Content) AddTenantButton(onClick = onAddTenantClicked)
        },
    ) { contentPadding ->
        when (uiState) {
            GroupUiState.Loading -> {
                LoadingIndicator(
                    description = stringResource(Res.string.group_loading),
                    modifier = Modifier.padding(contentPadding),
                )
            }

            is GroupUiState.Error -> {
                CenteredMessage(title = uiState.message.asString(), modifier = Modifier.padding(contentPadding))
            }

            is GroupUiState.Empty -> {
                NoTenants(
                    archived = uiState.archived,
                    onAddTenantClicked = onAddTenantClicked,
                    onArchivedClicked = onArchivedClicked,
                    contentPadding = contentPadding,
                )
            }

            is GroupUiState.Content -> {
                TenantList(
                    content = uiState,
                    onTenantClicked = onTenantClicked,
                    onArchivedClicked = onArchivedClicked,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

private fun GroupUiState.groupName(): String =
    when (this) {
        is GroupUiState.Content -> groupName
        is GroupUiState.Empty -> groupName
        GroupUiState.Loading, is GroupUiState.Error -> ""
    }

@Composable
private fun AddTenantButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
        text = { Text(stringResource(Res.string.group_add_tenant)) },
    )
}

@Composable
private fun TenantList(
    content: GroupUiState.Content,
    onTenantClicked: (tenantId: String) -> Unit,
    onArchivedClicked: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content.syncError?.let { error ->
            item(key = "syncError") { SyncErrorText(error) }
        }
        item(key = "summary") {
            GroupSummary(balance = content.balance, tenantCounts = content.tenantCounts)
        }
        content.archived?.let { archived ->
            item(key = "archived") { ArchivedTenantsLink(archived = archived, onClick = onArchivedClicked) }
        }
        item(key = "tenants") {
            TenantListCard(tenants = content.tenants) { tenant ->
                TenantRow(tenant = tenant, onClick = { onTenantClicked(tenant.tenantId) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupSummary(
    balance: UiText,
    tenantCounts: UiText,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = balance.asString(), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = tenantCounts.asString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun NoTenants(
    archived: UiText?,
    onAddTenantClicked: () -> Unit,
    onArchivedClicked: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        archived?.let {
            ArchivedTenantsLink(
                archived = it,
                onClick = onArchivedClicked,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        CenteredMessage(
            title = stringResource(Res.string.group_empty_title),
            body = stringResource(Res.string.group_empty_body),
        ) {
            Button(onClick = onAddTenantClicked, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(Res.string.group_add_tenant))
            }
        }
    }
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
                            TenantRowUiState(
                                tenantId = "t2",
                                name = "Apt 1B",
                                detail = UiText.Plain("Tom & Jess Ortega · Due Apr 1"),
                                balance = null,
                                chipState = TenantChipState.PaidUp,
                            ),
                        ),
                ),
            onBackClicked = {},
            onTenantClicked = {},
            onAddTenantClicked = {},
            onArchivedClicked = {},
        )
    }
}
