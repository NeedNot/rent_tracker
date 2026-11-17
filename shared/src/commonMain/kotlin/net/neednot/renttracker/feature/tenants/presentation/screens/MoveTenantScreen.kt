package net.neednot.renttracker.feature.tenants.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.ConfirmDialog
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EmptyState
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListItem
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.TenantStrip
import net.neednot.renttracker.core.designsystem.components.TopBarNavigation
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
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
import net.neednot.renttracker.resources.ic_chevron_right
import net.neednot.renttracker.resources.ic_drive_file_move
import net.neednot.renttracker.resources.move_confirm_move
import net.neednot.renttracker.resources.move_empty_body
import net.neednot.renttracker.resources.move_empty_title
import net.neednot.renttracker.resources.move_error_title
import net.neednot.renttracker.resources.move_tenant_intro
import net.neednot.renttracker.resources.move_tenant_strip
import net.neednot.renttracker.resources.move_tenant_title_generic
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
        onTargetClicked = { groupId ->
            viewModel.onTargetClicked(groupId)
            viewModel.onNextClicked()
        },
        onMoveConfirmed = viewModel::onMoveConfirmed,
        onConfirmationDismissed = viewModel::onConfirmationDismissed,
        onCloseClicked = onClosed,
        modifier = modifier,
    )
}

@Composable
internal fun MoveTenantContent(
    uiState: MoveTenantUiState,
    onTargetClicked: (groupId: String) -> Unit,
    onMoveConfirmed: () -> Unit,
    onConfirmationDismissed: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RtTopAppBar(
                title = stringResource(Res.string.move_tenant_title_generic),
                navigation = TopBarNavigation.Close,
                onNavigationClicked = onCloseClicked,
            )
        },
    ) { contentPadding ->
        val bodyModifier = Modifier.padding(contentPadding)
        when (uiState) {
            MoveTenantUiState.Loading -> {
                LoadingIndicator(bodyModifier)
            }

            is MoveTenantUiState.Error -> {
                ErrorState(
                    title = stringResource(Res.string.move_error_title),
                    text = uiState.message.asString(),
                    onRetryClicked = UnimplementedAction,
                    modifier = bodyModifier,
                )
            }

            MoveTenantUiState.Empty -> {
                EmptyState(
                    icon = Res.drawable.ic_drive_file_move,
                    title = stringResource(Res.string.move_empty_title),
                    modifier = bodyModifier,
                    text = stringResource(Res.string.move_empty_body),
                )
            }

            is MoveTenantUiState.Content -> {
                Targets(uiState, onTargetClicked, bodyModifier)
            }
        }
    }
    (uiState as? MoveTenantUiState.Content)?.confirmation?.let { confirmation ->
        MoveConfirmationDialog(
            confirmation = confirmation,
            isMoving = uiState.isMoving,
            onConfirmed = onMoveConfirmed,
            onDismissed = onConfirmationDismissed,
        )
    }
}

@Composable
private fun Targets(
    content: MoveTenantUiState.Content,
    onTargetClicked: (groupId: String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ContentLayout.List.padding),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
    ) {
        TargetsBody(content, onTargetClicked)
    }
}

@Composable
private fun TargetsBody(
    content: MoveTenantUiState.Content,
    onTargetClicked: (groupId: String) -> Unit,
) {
    TenantStrip(
        unit =
            content.contactName?.let { stringResource(Res.string.move_tenant_strip, content.tenantName, it) }
                ?: content.tenantName,
        balance = content.balance?.asString(),
    )
    Text(
        text = stringResource(Res.string.move_tenant_intro),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    RtListCard(items = content.targets) { target ->
        TargetRow(target = target, enabled = !content.isMoving, onClick = { onTargetClicked(target.groupId) })
    }
}

@Composable
private fun TargetRow(
    target: MoveTargetUiState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    RtListItem(
        text = target.name,
        supportingText = target.tenantCount.asString(),
        enabled = enabled,
        onClick = onClick,
        trailing = {
            AppIcon(
                icon = Res.drawable.ic_chevron_right,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun MoveConfirmationDialog(
    confirmation: MoveConfirmationUiState,
    isMoving: Boolean,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    ConfirmDialog(
        title = confirmation.title.asString(),
        text = confirmation.consequences.map { it.asString() }.joinToString(" "),
        confirmLabel = stringResource(Res.string.move_confirm_move),
        onConfirmed = onConfirmed,
        onDismissed = onDismissed,
        isConfirmEnabled = !isMoving,
    )
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
                            MoveTargetUiState("g3", "Shared property", UiText.Plain("2 tenants"), isSelected = false),
                        ),
                    contactName = "Dana Whitfield",
                    balance = UiText.Plain("Owes $550.00"),
                ),
            onTargetClicked = {},
            onMoveConfirmed = {},
            onConfirmationDismissed = {},
            onCloseClicked = {},
        )
    }
}
