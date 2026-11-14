package net.neednot.renttracker.feature.home.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.components.GroupCard
import net.neednot.renttracker.feature.home.presentation.uistate.GroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState
import net.neednot.renttracker.feature.home.presentation.viewmodel.HomeViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.app_name
import net.neednot.renttracker.resources.home_empty_body
import net.neednot.renttracker.resources.home_empty_title
import net.neednot.renttracker.resources.home_groups_heading
import net.neednot.renttracker.resources.home_loading
import net.neednot.renttracker.resources.home_new_group
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onGroupClicked: (groupId: String) -> Unit,
    onNewGroupClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeContent(
        uiState = uiState,
        onGroupClicked = onGroupClicked,
        onNewGroupClicked = onNewGroupClicked,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onGroupClicked: (groupId: String) -> Unit,
    onNewGroupClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.app_name)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewGroupClicked,
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.home_new_group)) },
            )
        },
    ) { contentPadding ->
        when (uiState) {
            HomeUiState.Loading -> {
                Loading(contentPadding)
            }

            HomeUiState.Empty -> {
                Message(
                    title = stringResource(Res.string.home_empty_title),
                    body = stringResource(Res.string.home_empty_body),
                    contentPadding = contentPadding,
                )
            }

            is HomeUiState.Error -> {
                Message(title = uiState.message.asString(), body = null, contentPadding = contentPadding)
            }

            is HomeUiState.Content -> {
                GroupList(
                    groups = uiState.groups,
                    syncError = uiState.syncError,
                    onGroupClicked = onGroupClicked,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
private fun GroupList(
    groups: List<GroupCardUiState>,
    syncError: UiText?,
    onGroupClicked: (groupId: String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        syncError?.let { error ->
            item(key = "syncError") {
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item(key = "heading") {
            Text(
                text = stringResource(Res.string.home_groups_heading),
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        items(groups, key = { it.groupId }) { group ->
            GroupCard(group = group, onClick = { onGroupClicked(group.groupId) })
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
    body: String?,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
        body?.let {
            Text(
                text = it,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview
@Composable
private fun HomeContentPreview() {
    RentTrackerTheme {
        HomeContent(
            uiState =
                HomeUiState.Content(
                    groups =
                        listOf(
                            GroupCardUiState(
                                groupId = "g1",
                                name = "Maple Court",
                                balance = UiText.Plain("Paid up"),
                                isPaidUp = true,
                                tenantCount = UiText.Plain("4 tenants · none late"),
                                lateCount = null,
                                archivedOwed = null,
                            ),
                        ),
                ),
            onGroupClicked = {},
            onNewGroupClicked = {},
        )
    }
}
