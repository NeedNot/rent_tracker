package net.neednot.renttracker.feature.tenants.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.navigation.LedgerRoute
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerEntryHost
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerRow
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryEditTarget
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.LedgerViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_empty_title
import net.neednot.renttracker.resources.ledger_log_payment
import net.neednot.renttracker.resources.ledger_subtitle
import net.neednot.renttracker.resources.ledger_title
import net.neednot.renttracker.resources.tenant_back
import net.neednot.renttracker.resources.tenant_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LedgerScreen(
    route: LedgerRoute,
    onBackClicked: () -> Unit,
    onLogPaymentClicked: () -> Unit,
    onEditEntryClicked: (EntryEditTarget) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LedgerViewModel = koinViewModel { parametersOf(route) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LedgerEntryHost(
        tenantId = route.tenantId,
        onEditClicked = onEditEntryClicked,
        content = { onRowClicked, onRestoreClicked ->
            LedgerContent(
                uiState = uiState,
                onBackClicked = onBackClicked,
                onLogPaymentClicked = onLogPaymentClicked,
                onRowClicked = onRowClicked,
                onRestoreClicked = onRestoreClicked,
                modifier = modifier,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LedgerContent(
    uiState: LedgerUiState,
    onBackClicked: () -> Unit,
    onLogPaymentClicked: () -> Unit,
    onRowClicked: (key: String) -> Unit,
    onRestoreClicked: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { LedgerTitle(uiState as? LedgerUiState.Content) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.tenant_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        when (uiState) {
            LedgerUiState.Loading -> {
                Loading(contentPadding)
            }

            is LedgerUiState.Error -> {
                Message(uiState.message, contentPadding)
            }

            is LedgerUiState.Content -> {
                LedgerBody(uiState, onLogPaymentClicked, onRowClicked, onRestoreClicked, contentPadding)
            }
        }
    }
}

@Composable
private fun LedgerTitle(content: LedgerUiState.Content?) {
    Column {
        Text(text = stringResource(Res.string.ledger_title), style = MaterialTheme.typography.titleLarge)
        content?.let {
            Text(
                text = stringResource(Res.string.ledger_subtitle, it.tenantName, it.groupName),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LedgerBody(
    content: LedgerUiState.Content,
    onLogPaymentClicked: () -> Unit,
    onRowClicked: (key: String) -> Unit,
    onRestoreClicked: (key: String) -> Unit,
    contentPadding: PaddingValues,
) {
    if (content.emptyMessage != null) {
        EmptyLedger(
            message = content.emptyMessage,
            canLogPayment = content.canLogPayment,
            onLogPaymentClicked = onLogPaymentClicked,
            contentPadding = contentPadding,
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        content.syncError?.let { error ->
            item {
                Text(
                    text = error.asString(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        content.sections.forEach { section ->
            item(key = section.header) {
                Text(
                    text = section.header,
                    modifier =
                        Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                            .semantics { heading() },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(section.rows, key = { it.key }) { row ->
                LedgerRow(
                    row = row,
                    onClicked = { onRowClicked(row.key) },
                    onRestoreClicked = { onRestoreClicked(row.key) },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun EmptyLedger(
    message: UiText,
    canLogPayment: Boolean,
    onLogPaymentClicked: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.ledger_empty_title),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = message.asString(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (canLogPayment) {
            Button(onClick = onLogPaymentClicked, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.ledger_log_payment))
            }
        }
    }
}

@Composable
private fun Loading(contentPadding: PaddingValues) {
    val description = stringResource(Res.string.tenant_loading)
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun Message(
    message: UiText,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message.asString(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
    }
}
