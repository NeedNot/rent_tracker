package net.neednot.renttracker.feature.home.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.uistate.CreateGroupUiState
import net.neednot.renttracker.feature.home.presentation.viewmodel.CreateGroupEvent
import net.neednot.renttracker.feature.home.presentation.viewmodel.CreateGroupViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.create_group_close
import net.neednot.renttracker.resources.create_group_name_label
import net.neednot.renttracker.resources.create_group_save
import net.neednot.renttracker.resources.create_group_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateGroupScreen(
    onGroupCreated: (groupId: String) -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateGroupViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateGroupEvent.GroupCreated -> onGroupCreated(event.groupId)
            }
        }
    }
    CreateGroupContent(
        uiState = uiState,
        onNameChanged = viewModel::onNameChanged,
        onSaveClicked = viewModel::onSaveClicked,
        onCloseClicked = onCloseClicked,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupContent(
    uiState: CreateGroupUiState,
    onNameChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.create_group_title)) },
                navigationIcon = {
                    IconButton(onClick = onCloseClicked) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.create_group_close),
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onSaveClicked,
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = !uiState.isSaving,
                    ) {
                        Text(stringResource(Res.string.create_group_save))
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                label = { Text(stringResource(Res.string.create_group_name_label)) },
                supportingText = uiState.error?.let { error -> { Text(error.asString()) } },
                isError = uiState.error != null,
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSaveClicked() }),
            )
        }
    }
}

@Preview
@Composable
private fun CreateGroupContentPreview() {
    RentTrackerTheme {
        CreateGroupContent(
            uiState = CreateGroupUiState(name = "1st Street"),
            onNameChanged = {},
            onSaveClicked = {},
            onCloseClicked = {},
        )
    }
}
