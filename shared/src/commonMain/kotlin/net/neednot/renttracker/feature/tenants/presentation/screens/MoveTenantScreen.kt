package net.neednot.renttracker.feature.tenants.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.navigation.MoveTenantRoute
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveConfirmationUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveTargetUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveTenantUiState
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.MoveTenantEvent
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.MoveTenantViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.common_close_without_saving
import net.neednot.renttracker.resources.move_confirm_move
import net.neednot.renttracker.resources.move_empty_back
import net.neednot.renttracker.resources.move_empty_body
import net.neednot.renttracker.resources.move_empty_title
import net.neednot.renttracker.resources.move_tenant_intro
import net.neednot.renttracker.resources.move_tenant_next
import net.neednot.renttracker.resources.move_tenant_title
import net.neednot.renttracker.resources.move_tenant_title_generic
import net.neednot.renttracker.resources.tenant_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MoveTenantScreen(
    route: MoveTenantRoute,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoveTenantViewModel = koinViewModel { parametersOf(route) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MoveTenantEvent.TenantMoved -> onClosed()
            }
        }
    }
    MoveTenantContent(
        uiState = uiState,
        onTargetClicked = viewModel::onTargetClicked,
        onNextClicked = viewModel::onNextClicked,
        onMoveConfirmed = viewModel::onMoveConfirmed,
        onConfirmationDismissed = viewModel::onConfirmationDismissed,
        onCloseClicked = onClosed,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveTenantContent(
    uiState: MoveTenantUiState,
    onTargetClicked: (groupId: String) -> Unit,
    onNextClicked: () -> Unit,
    onMoveConfirmed: () -> Unit,
    onConfirmationDismissed: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.title()) },
                navigationIcon = {
                    IconButton(onClick = onCloseClicked) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.common_close_without_saving),
                        )
                    }
                },
                actions = {
                    if (uiState is MoveTenantUiState.Content) {
                        Button(
                            onClick = onNextClicked,
                            modifier = Modifier.padding(end = 8.dp),
                            enabled = !uiState.isMoving,
                        ) {
                            Text(stringResource(Res.string.move_tenant_next))
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        when (uiState) {
            MoveTenantUiState.Loading -> {
                Loading(contentPadding)
            }

            is MoveTenantUiState.Error -> {
                ErrorMessage(message = uiState.message, contentPadding = contentPadding)
            }

            is MoveTenantUiState.Empty -> {
                NoTargets(
                    tenantName = uiState.tenantName,
                    onBackClicked = onCloseClicked,
                    contentPadding = contentPadding,
                )
            }

            is MoveTenantUiState.Content -> {
                TargetList(
                    targets = uiState.targets,
                    onTargetClicked = onTargetClicked,
                    contentPadding = contentPadding,
                )
            }
        }
    }
    (uiState as? MoveTenantUiState.Content)?.confirmation?.let { confirmation ->
        MoveConfirmationDialog(
            confirmation = confirmation,
            onConfirmed = onMoveConfirmed,
            onDismissed = onConfirmationDismissed,
        )
    }
}

@Composable
private fun MoveTenantUiState.title(): String =
    when (this) {
        is MoveTenantUiState.Content -> stringResource(Res.string.move_tenant_title, tenantName)
        is MoveTenantUiState.Empty -> stringResource(Res.string.move_tenant_title, tenantName)
        is MoveTenantUiState.Error, MoveTenantUiState.Loading -> stringResource(Res.string.move_tenant_title_generic)
    }

@Composable
private fun TargetList(
    targets: List<MoveTargetUiState>,
    onTargetClicked: (groupId: String) -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.move_tenant_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            targets.forEachIndexed { index, target ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                TargetRow(target = target, onClick = { onTargetClicked(target.groupId) })
            }
        }
    }
}

@Composable
private fun TargetRow(
    target: MoveTargetUiState,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = target.isSelected, onClick = onClick, role = Role.RadioButton)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = target.isSelected, onClick = null)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = target.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = target.tenantCount.asString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun NoTargets(
    tenantName: String,
    onBackClicked: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Icon(imageVector = Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.padding(20.dp))
        }
        Text(
            text = stringResource(Res.string.move_empty_title),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(Res.string.move_empty_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onBackClicked) { Text(stringResource(Res.string.move_empty_back, tenantName)) }
    }
}

@Composable
private fun MoveConfirmationDialog(
    confirmation: MoveConfirmationUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = { TextButton(onClick = onConfirmed) { Text(stringResource(Res.string.move_confirm_move)) } },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(Res.string.common_cancel)) } },
        title = { Text(confirmation.title.asString()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                confirmation.consequences.forEach { consequence ->
                    Text(text = "• " + consequence.asString(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
    )
}

@Composable
private fun Loading(contentPadding: PaddingValues) {
    val description = stringResource(Res.string.tenant_loading)
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun ErrorMessage(
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

@Preview
@Composable
private fun MoveTenantContentPreview() {
    RentTrackerTheme {
        MoveTenantContent(
            uiState =
                MoveTenantUiState.Content(
                    tenantName = "Apt 3B",
                    targets =
                        listOf(
                            MoveTargetUiState("g2", "Maple Court", UiText.Plain("4 tenants"), isSelected = true),
                            MoveTargetUiState(
                                "g3",
                                "Elm Street Fourplex",
                                UiText.Plain("3 tenants"),
                                isSelected = false,
                            ),
                        ),
                ),
            onTargetClicked = {},
            onNextClicked = {},
            onMoveConfirmed = {},
            onConfirmationDismissed = {},
            onCloseClicked = {},
        )
    }
}
