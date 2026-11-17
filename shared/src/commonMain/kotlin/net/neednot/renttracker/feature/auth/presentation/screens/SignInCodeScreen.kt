package net.neednot.renttracker.feature.auth.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.FieldFoot
import net.neednot.renttracker.core.designsystem.components.OtpField
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.auth.presentation.components.AuthScreenLayout
import net.neednot.renttracker.feature.auth.presentation.uistate.SignInCodeUiState
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInCodeEvent
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInCodeViewModel
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignedInDestination
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_code_sent_to
import net.neednot.renttracker.resources.auth_code_title
import net.neednot.renttracker.resources.auth_continue
import net.neednot.renttracker.resources.auth_send_new_code
import net.neednot.renttracker.resources.auth_send_new_code_in
import net.neednot.renttracker.resources.auth_use_different_email
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val CODE_LENGTH = 6

@Composable
fun SignInCodeScreen(
    email: String,
    onSignedIn: (SignedInDestination) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInCodeViewModel = koinViewModel { parametersOf(email) },
) {
    val uiState by viewModel.uiState.collectAsState()
    val codeState = rememberTextFieldState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SignInCodeEvent.SignedIn -> onSignedIn(event.destination)
            }
        }
    }
    SignInCodeContent(
        uiState = uiState,
        codeState = codeState,
        onVerifyClicked = {
            viewModel.onVerifyClicked(codeState.text.toString())
            codeState.clearText()
        },
        onSendNewCodeClicked = viewModel::onSendNewCodeClicked,
        onBackClicked = onBackClicked,
        modifier = modifier,
    )
}

@Composable
internal fun SignInCodeContent(
    uiState: SignInCodeUiState,
    codeState: TextFieldState,
    onVerifyClicked: () -> Unit,
    onSendNewCodeClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOffline = uiState.offlineBanner != null
    val canVerify = !uiState.isBusy && !isOffline && codeState.text.length == CODE_LENGTH
    AuthScreenLayout(
        modifier = modifier,
        offlineBanner = uiState.offlineBanner?.asString(),
        topBar = { RtTopAppBar(title = "", onNavigationClicked = onBackClicked) },
    ) {
        Text(
            text = stringResource(Res.string.auth_code_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.auth_code_sent_to, uiState.email),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            OtpField(
                state = codeState,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null,
                enabled = !uiState.isVerifying,
                onDone = { if (canVerify) onVerifyClicked() },
            )
            FieldFoot(error = uiState.error?.asString())
        }
        CodeLinks(uiState = uiState, onSendNewCodeClicked = onSendNewCodeClicked, onBackClicked = onBackClicked)
        Spacer(modifier = Modifier.weight(1f))
        RtButton(
            text = stringResource(Res.string.auth_continue),
            onClick = onVerifyClicked,
            modifier = Modifier.fillMaxWidth(),
            enabled = canVerify || uiState.isVerifying,
            isWorking = uiState.isVerifying,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodeLinks(
    uiState: SignInCodeUiState,
    onSendNewCodeClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        RtButton(
            text =
                if (uiState.secondsUntilNewCode > 0) {
                    stringResource(Res.string.auth_send_new_code_in, uiState.secondsUntilNewCode)
                } else {
                    stringResource(Res.string.auth_send_new_code)
                },
            onClick = onSendNewCodeClicked,
            variant = ButtonVariant.Text,
            enabled = uiState.canSendNewCode || uiState.isSendingNewCode,
            isWorking = uiState.isSendingNewCode,
        )
        RtButton(
            text = stringResource(Res.string.auth_use_different_email),
            onClick = onBackClicked,
            variant = ButtonVariant.Text,
            enabled = !uiState.isBusy,
        )
    }
}

@Preview
@Composable
private fun SignInCodeContentPreview() {
    RentTrackerTheme {
        SignInCodeContent(
            uiState = SignInCodeUiState(email = "you@example.com"),
            codeState = rememberTextFieldState("481"),
            onVerifyClicked = {},
            onSendNewCodeClicked = {},
            onBackClicked = {},
        )
    }
}
