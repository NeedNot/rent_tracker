package net.neednot.renttracker.feature.group.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EmptyState
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.components.RowAmount
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListRow
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantRowUiState
import net.neednot.renttracker.feature.group.presentation.uistate.ArchivedTenantsUiState
import net.neednot.renttracker.feature.group.presentation.viewmodel.ArchivedTenantsViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.archived_tenants_empty
import net.neednot.renttracker.resources.archived_tenants_error_title
import net.neednot.renttracker.resources.archived_tenants_title
import net.neednot.renttracker.resources.ic_inventory_2
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ArchivedTenantsScreen(
    groupId: String,
    onBackClicked: () -> Unit,
    onTenantClicked: (tenantId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchivedTenantsViewModel = koinViewModel { parametersOf(groupId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    ArchivedTenantsContent(
        uiState = uiState,
        onBackClicked = onBackClicked,
        onTenantClicked = onTenantClicked,
        modifier = modifier,
    )
}

@Composable
internal fun ArchivedTenantsContent(
    uiState: ArchivedTenantsUiState,
    onBackClicked: () -> Unit,
    onTenantClicked: (tenantId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RtTopAppBar(title = stringResource(Res.string.archived_tenants_title), onNavigationClicked = onBackClicked)
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            uiState.offlineBanner?.let { OfflineBanner(text = it.asString()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = ContentLayout.List.padding,
                verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
            ) {
                archivedTenantsBody(uiState = uiState, onTenantClicked = onTenantClicked)
            }
        }
    }
}

private const val LOADING_HEIGHT_FRACTION = 0.6f

private fun LazyListScope.archivedTenantsBody(
    uiState: ArchivedTenantsUiState,
    onTenantClicked: (tenantId: String) -> Unit,
) {
    when (uiState) {
        is ArchivedTenantsUiState.Loading -> {
            item(key = "loading") { LoadingIndicator(Modifier.fillParentMaxHeight(LOADING_HEIGHT_FRACTION)) }
        }

        is ArchivedTenantsUiState.Empty -> {
            item(key = "empty") {
                EmptyState(
                    icon = Res.drawable.ic_inventory_2,
                    title = stringResource(Res.string.archived_tenants_empty),
                )
            }
        }

        is ArchivedTenantsUiState.Error -> {
            item(key = "error") {
                ErrorState(
                    title = stringResource(Res.string.archived_tenants_error_title),
                    text = uiState.message.asString(),
                    onRetryClicked = UnimplementedAction,
                )
            }
        }

        is ArchivedTenantsUiState.Content -> {
            uiState.syncError?.let { error ->
                item(key = "syncError") { Banner(text = error.asString(), kind = BannerKind.Error) }
            }
            item(key = "tenants") {
                RtListCard(items = uiState.tenants) { tenant ->
                    ArchivedTenantRow(tenant = tenant, onClick = { onTenantClicked(tenant.tenantId) })
                }
            }
        }
    }
}

@Composable
private fun ArchivedTenantRow(
    tenant: ArchivedTenantRowUiState,
    onClick: () -> Unit,
) {
    RtListRow(
        title = tenant.name,
        subtitle = tenant.contactName,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
        trailing = { RowAmount(text = tenant.balance.asString()) },
    )
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
