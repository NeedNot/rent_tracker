package net.neednot.renttracker.feature.tenants.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EmptyState
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.SectionHeader
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.navigation.LedgerRoute
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerEntryHost
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerItem
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerItemActions
import net.neednot.renttracker.feature.tenants.presentation.components.PendingPaymentHost
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryEditTarget
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.LedgerViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_receipt_long
import net.neednot.renttracker.resources.ledger_empty_title
import net.neednot.renttracker.resources.ledger_error_title
import net.neednot.renttracker.resources.ledger_log_payment
import net.neednot.renttracker.resources.ledger_subtitle
import net.neednot.renttracker.resources.ledger_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LedgerScreen(
    route: LedgerRoute,
    onBackClicked: () -> Unit,
    onLogPaymentClicked: () -> Unit,
    onEditEntryClicked: (EntryEditTarget) -> Unit,
    onEditPendingPaymentClicked: (clientId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LedgerViewModel = koinViewModel { parametersOf(route) },
) {
    val uiState by viewModel.uiState.collectAsState()
    PendingPaymentHost(
        route = TenantRoute(route.tenantId),
        onEditClicked = onEditPendingPaymentClicked,
    ) { onPendingClicked ->
        LedgerEntryHost(
            tenantId = route.tenantId,
            onEditClicked = onEditEntryClicked,
            content = { onRowClicked, onRestoreClicked ->
                LedgerContent(
                    uiState = uiState,
                    onBackClicked = onBackClicked,
                    onLogPaymentClicked = onLogPaymentClicked,
                    itemActions = LedgerItemActions(onRowClicked, onRestoreClicked, onPendingClicked),
                    modifier = modifier,
                )
            },
        )
    }
}

@Composable
internal fun LedgerContent(
    uiState: LedgerUiState,
    onBackClicked: () -> Unit,
    onLogPaymentClicked: () -> Unit,
    itemActions: LedgerItemActions,
    modifier: Modifier = Modifier,
) {
    val content = uiState as? LedgerUiState.Content
    Scaffold(
        modifier = modifier,
        topBar = {
            RtTopAppBar(
                title = stringResource(Res.string.ledger_title),
                subtitle = content?.let { stringResource(Res.string.ledger_subtitle, it.tenantName, it.groupName) },
                onNavigationClicked = onBackClicked,
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            content?.offlineBanner?.let { OfflineBanner(text = it.asString()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = ContentLayout.List.padding,
                verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
            ) {
                when (uiState) {
                    LedgerUiState.Loading -> {
                        item(
                            key = "loading",
                        ) { LoadingIndicator(Modifier.fillParentMaxHeight(LOADING_HEIGHT_FRACTION)) }
                    }

                    is LedgerUiState.Error -> {
                        item(key = "error") {
                            ErrorState(
                                title = stringResource(Res.string.ledger_error_title),
                                text = uiState.message.asString(),
                                onRetryClicked = UnimplementedAction,
                            )
                        }
                    }

                    is LedgerUiState.Content -> {
                        ledgerItems(uiState, onLogPaymentClicked, itemActions)
                    }
                }
            }
        }
    }
}

private const val LOADING_HEIGHT_FRACTION = 0.6f

private fun LazyListScope.ledgerItems(
    content: LedgerUiState.Content,
    onLogPaymentClicked: () -> Unit,
    itemActions: LedgerItemActions,
) {
    content.syncError?.let { error ->
        item(key = "syncError") { Banner(text = error.asString(), kind = BannerKind.Error) }
    }
    content.emptyMessage?.let { message ->
        item(key = "empty") {
            EmptyState(
                icon = Res.drawable.ic_receipt_long,
                title = stringResource(Res.string.ledger_empty_title),
                text = message.asString(),
                action =
                    if (content.canLogPayment) {
                        {
                            RtButton(
                                text = stringResource(Res.string.ledger_log_payment),
                                onClick = onLogPaymentClicked,
                                variant = ButtonVariant.Outlined,
                            )
                        }
                    } else {
                        null
                    },
            )
        }
    }
    content.sections.forEach { section ->
        item(key = "header:${section.header}") { SectionHeader(text = section.header) }
        item(key = "rows:${section.header}") {
            RtListCard(items = section.rows) { row -> LedgerItem(item = row, actions = itemActions) }
        }
    }
    content.pendingNote?.let { note ->
        item(key = "pendingNote") {
            Text(
                text = note.asString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
