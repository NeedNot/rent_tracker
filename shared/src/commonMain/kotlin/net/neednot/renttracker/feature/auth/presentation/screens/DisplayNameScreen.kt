package net.neednot.renttracker.feature.auth.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.TopBarNavigation
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.auth.presentation.components.AuthScreenLayout
import net.neednot.renttracker.feature.auth.presentation.uistate.DisplayNameUiState
import net.neednot.renttracker.feature.auth.presentation.viewmodel.DisplayNameEvent
import net.neednot.renttracker.feature.auth.presentation.viewmodel.DisplayNameViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_continue
import net.neednot.renttracker.resources.auth_display_name_label
import net.neednot.renttracker.resources.auth_display_name_subtitle
import net.neednot.renttracker.resources.auth_display_name_title
import net.neednot.renttracker.resources.common_saving
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
internal fun DisplayNameContent(
    uiState: DisplayNameUiState,
    onDisplayNameChanged: (String) -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenLayout(
        modifier = modifier,
        offlineBanner = uiState.offlineBanner?.asString(),
        topBar = { RtTopAppBar(title = "", navigation = TopBarNavigation.None) },
    ) {
        Text(
            text = stringResource(Res.string.auth_display_name_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.auth_display_name_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        FormTextField(
            label = stringResource(Res.string.auth_display_name_label),
            text = uiState.displayName,
            onTextChanged = onDisplayNameChanged,
            error = uiState.error,
            enabled = !uiState.isSaving,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { onContinueClicked() }),
            contentType = ContentType.PersonFullName,
        )
        Spacer(modifier = Modifier.weight(1f))
        RtButton(
            text = stringResource(if (uiState.isSaving) Res.string.common_saving else Res.string.auth_continue),
            onClick = onContinueClicked,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.offlineBanner == null,
            isWorking = uiState.isSaving,
        )
    }
}

@Preview
@Composable
private fun DisplayNameContentPreview() {
    RentTrackerTheme {
        DisplayNameContent(
            uiState = DisplayNameUiState(displayName = "Need_Not"),
            onDisplayNameChanged = {},
            onContinueClicked = {},
        )
    }
}
