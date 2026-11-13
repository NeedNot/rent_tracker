package net.neednot.renttracker.feature.auth.presentation.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.auth.presentation.components.AuthButtonProgress
import net.neednot.renttracker.feature.auth.presentation.components.AuthScreenLayout
import net.neednot.renttracker.feature.auth.presentation.uistate.DisplayNameUiState
import net.neednot.renttracker.feature.auth.presentation.viewmodel.DisplayNameEvent
import net.neednot.renttracker.feature.auth.presentation.viewmodel.DisplayNameViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_display_name_continue
import net.neednot.renttracker.resources.auth_display_name_label
import net.neednot.renttracker.resources.auth_display_name_subtitle
import net.neednot.renttracker.resources.auth_display_name_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DisplayNameScreen(
    defaultDisplayName: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DisplayNameViewModel = koinViewModel { parametersOf(defaultDisplayName) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DisplayNameEvent.Done -> onDone()
            }
        }
    }
    DisplayNameContent(
        uiState = uiState,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        onContinueClicked = viewModel::onContinueClicked,
        modifier = modifier,
    )
}

@Composable
private fun DisplayNameContent(
    uiState: DisplayNameUiState,
    onDisplayNameChanged: (String) -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenLayout(modifier = modifier) {
        Text(
            text = stringResource(Res.string.auth_display_name_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.auth_display_name_subtitle),
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = onDisplayNameChanged,
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.PersonFullName },
            enabled = !uiState.isSaving,
            label = { Text(stringResource(Res.string.auth_display_name_label)) },
            supportingText = uiState.error?.let { error -> { Text(error.asString()) } },
            isError = uiState.error != null,
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onContinueClicked() }),
        )
        Button(
            onClick = onContinueClicked,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !uiState.isSaving,
        ) {
            if (uiState.isSaving) AuthButtonProgress()
            Text(stringResource(Res.string.auth_display_name_continue))
        }
    }
}

@Preview
@Composable
private fun DisplayNameContentPreview() {
    RentTrackerTheme {
        DisplayNameContent(
            uiState = DisplayNameUiState(displayName = "dalton.h"),
            onDisplayNameChanged = {},
            onContinueClicked = {},
        )
    }
}
