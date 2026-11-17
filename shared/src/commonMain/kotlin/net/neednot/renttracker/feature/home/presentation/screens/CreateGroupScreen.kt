package net.neednot.renttracker.feature.home.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EditorTopBar
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.feature.home.presentation.uistate.CreateGroupUiState
import net.neednot.renttracker.feature.home.presentation.viewmodel.CreateGroupEvent
import net.neednot.renttracker.feature.home.presentation.viewmodel.CreateGroupViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.create_group_name_label
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
            EditorTopBar(
                title = stringResource(Res.string.create_group_title),
                isSaveEnabled = !uiState.isSaving,
                onSaveClicked = onSaveClicked,
                onCloseClicked = onCloseClicked,
                isSaving = uiState.isSaving,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(ContentLayout.Form.padding),
            verticalArrangement = Arrangement.spacedBy(ContentLayout.Form.spacing),
        ) {
            FormTextField(
                label = stringResource(Res.string.create_group_name_label),
                text = uiState.name,
                onTextChanged = onNameChanged,
                error = uiState.error,
                enabled = !uiState.isSaving,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
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
