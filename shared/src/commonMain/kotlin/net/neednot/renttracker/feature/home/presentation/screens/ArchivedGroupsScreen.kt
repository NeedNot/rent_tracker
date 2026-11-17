package net.neednot.renttracker.feature.home.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EmptyState
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListItem
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.uistate.ArchivedGroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.ArchivedGroupsUiState
import net.neednot.renttracker.feature.home.presentation.viewmodel.ArchivedGroupsViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.archived_groups_empty
import net.neednot.renttracker.resources.archived_groups_error_title
import net.neednot.renttracker.resources.archived_groups_help
import net.neednot.renttracker.resources.archived_groups_title
import net.neednot.renttracker.resources.ic_chevron_right
import net.neednot.renttracker.resources.ic_inventory_2
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ArchivedGroupsScreen(
    onBackClicked: () -> Unit,
    onGroupClicked: (groupId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchivedGroupsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ArchivedGroupsContent(
        uiState = uiState,
        onBackClicked = onBackClicked,
        onGroupClicked = onGroupClicked,
        modifier = modifier,
    )
}

@Composable
private fun ArchivedGroupsContent(
    uiState: ArchivedGroupsUiState,
    onBackClicked: () -> Unit,
    onGroupClicked: (groupId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RtTopAppBar(
                title = stringResource(Res.string.archived_groups_title),
                onNavigationClicked = onBackClicked,
            )
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            when (uiState) {
                ArchivedGroupsUiState.Loading -> {
                    LoadingIndicator()
                }

                ArchivedGroupsUiState.Empty -> {
                    EmptyState(
                        icon = Res.drawable.ic_inventory_2,
                        title = stringResource(Res.string.archived_groups_empty),
                    )
                }

                is ArchivedGroupsUiState.Error -> {
                    ErrorState(
                        title = stringResource(Res.string.archived_groups_error_title),
                        text = uiState.message.asString(),
                        onRetryClicked = UnimplementedAction,
                    )
                }

                is ArchivedGroupsUiState.Content -> {
                    ArchivedGroupList(content = uiState, onGroupClicked = onGroupClicked)
                }
            }
        }
    }
}

@Composable
private fun ArchivedGroupList(
    content: ArchivedGroupsUiState.Content,
    onGroupClicked: (groupId: String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ContentLayout.List.padding),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
    ) {
        content.syncError?.let { error -> Banner(text = error.asString(), kind = BannerKind.Error) }
        Text(
            text = stringResource(Res.string.archived_groups_help),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        RtListCard(items = content.groups) { group ->
            RtListItem(
                text = group.name,
                supportingText = group.summary.asString(),
                onClick = { onGroupClicked(group.groupId) },
                trailing = {
                    AppIcon(
                        icon = Res.drawable.ic_chevron_right,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun ArchivedGroupsContentPreview() {
    RentTrackerTheme {
        ArchivedGroupsContent(
            uiState =
                ArchivedGroupsUiState.Content(
                    groups =
                        listOf(
                            ArchivedGroupCardUiState(
                                groupId = "g1",
                                name = "Old Mill Road",
                                summary = UiText.Plain("Archived Jan 5, 2026 · 3 tenants"),
                            ),
                        ),
                ),
            onBackClicked = {},
            onGroupClicked = {},
        )
    }
}
