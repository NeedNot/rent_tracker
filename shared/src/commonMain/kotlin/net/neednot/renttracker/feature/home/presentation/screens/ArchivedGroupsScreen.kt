package net.neednot.renttracker.feature.home.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.components.ArchivedGroupCard
import net.neednot.renttracker.feature.home.presentation.uistate.ArchivedGroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.ArchivedGroupsUiState
import net.neednot.renttracker.feature.home.presentation.viewmodel.ArchivedGroupsViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.archived_groups_empty
import net.neednot.renttracker.resources.archived_groups_subtitle
import net.neednot.renttracker.resources.archived_groups_title
import net.neednot.renttracker.resources.home_loading
import net.neednot.renttracker.resources.tenant_back
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

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = { ArchivedGroupsTitle() },
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
            ArchivedGroupsUiState.Loading -> {
                Loading(contentPadding)
            }

            ArchivedGroupsUiState.Empty -> {
                Message(stringResource(Res.string.archived_groups_empty), contentPadding)
            }

            is ArchivedGroupsUiState.Error -> {
                Message(uiState.message.asString(), contentPadding)
            }

            is ArchivedGroupsUiState.Content -> {
                ArchivedGroupList(content = uiState, onGroupClicked = onGroupClicked, contentPadding = contentPadding)
            }
        }
    }
}

@Composable
private fun ArchivedGroupsTitle() {
    Column {
        Text(text = stringResource(Res.string.archived_groups_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(Res.string.archived_groups_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ArchivedGroupList(
    content: ArchivedGroupsUiState.Content,
    onGroupClicked: (groupId: String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content.syncError?.let { error ->
            item(key = "syncError") {
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(content.groups, key = { it.groupId }) { group ->
            ArchivedGroupCard(group = group, onClick = { onGroupClicked(group.groupId) })
        }
    }
}

@Composable
private fun Loading(contentPadding: PaddingValues) {
    val description = stringResource(Res.string.home_loading)
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun Message(
    title: String,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
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
                                balance = UiText.Plain("Owes $900.00"),
                                summary = UiText.Plain("3 tenants · archived Jan 5, 2026"),
                            ),
                        ),
                ),
            onBackClicked = {},
            onGroupClicked = {},
        )
    }
}
