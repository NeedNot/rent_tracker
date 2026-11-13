package net.neednot.renttracker.feature.group.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.components.ArchivedTenantRow
import net.neednot.renttracker.feature.group.presentation.components.CenteredMessage
import net.neednot.renttracker.feature.group.presentation.components.LoadingIndicator
import net.neednot.renttracker.feature.group.presentation.components.SyncErrorText
import net.neednot.renttracker.feature.group.presentation.components.TenantListCard
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantRowUiState
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantsUiState
import net.neednot.renttracker.feature.group.presentation.viewmodel.ArchivedTenantsViewModel
import net.neednot.renttracker.feature.group.presentation.viewmodel.GroupEvent
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.archived_tenants_empty
import net.neednot.renttracker.resources.archived_tenants_title
import net.neednot.renttracker.resources.group_back
import net.neednot.renttracker.resources.group_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ArchivedTenantsScreen(
    groupId: String,
    onBackClicked: () -> Unit,
    onTenantClicked: (tenantId: String) -> Unit,
    onGroupGone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchivedTenantsViewModel = koinViewModel { parametersOf(groupId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GroupEvent.GroupGone -> onGroupGone()
            }
        }
    }
    ArchivedTenantsContent(
        uiState = uiState,
        onBackClicked = onBackClicked,
        onTenantClicked = onTenantClicked,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchivedTenantsContent(
    uiState: ArchivedTenantsUiState,
    onBackClicked: () -> Unit,
    onTenantClicked: (tenantId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.archived_tenants_title)) },
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
    ) { contentPadding ->
        when (uiState) {
            ArchivedTenantsUiState.Loading -> {
                LoadingIndicator(
                    description = stringResource(Res.string.group_loading),
                    modifier = Modifier.padding(contentPadding),
                )
            }

            ArchivedTenantsUiState.Empty -> {
                CenteredMessage(
                    title = stringResource(Res.string.archived_tenants_empty),
                    modifier = Modifier.padding(contentPadding),
                )
            }

            is ArchivedTenantsUiState.Error -> {
                CenteredMessage(title = uiState.message.asString(), modifier = Modifier.padding(contentPadding))
            }

            is ArchivedTenantsUiState.Content -> {
                ArchivedTenantList(
                    content = uiState,
                    onTenantClicked = onTenantClicked,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
private fun ArchivedTenantList(
    content: ArchivedTenantsUiState.Content,
    onTenantClicked: (tenantId: String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content.syncError?.let { error ->
            item(key = "syncError") { SyncErrorText(error) }
        }
        item(key = "tenants") {
            TenantListCard(tenants = content.tenants) { tenant ->
                ArchivedTenantRow(tenant = tenant, onClick = { onTenantClicked(tenant.tenantId) })
            }
        }
    }
}

@Preview
@Composable
private fun ArchivedTenantsContentPreview() {
    RentTrackerTheme {
        ArchivedTenantsContent(
            uiState =
                ArchivedTenantsUiState.Content(
                    tenants =
                        listOf(
                            ArchivedTenantRowUiState(
                                tenantId = "t1",
                                name = "Apt 4C",
                                contactName = "Sam Park",
                                balance = UiText.Plain("Owes $2,400.00"),
                            ),
                        ),
                ),
            onBackClicked = {},
            onTenantClicked = {},
        )
    }
}
