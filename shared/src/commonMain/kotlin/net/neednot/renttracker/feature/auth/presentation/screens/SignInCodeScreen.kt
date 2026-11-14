package net.neednot.renttracker.feature.auth.presentation.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.OtpField
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.feature.auth.presentation.components.AuthButtonProgress
import net.neednot.renttracker.feature.auth.presentation.components.AuthErrorText
import net.neednot.renttracker.feature.auth.presentation.components.AuthScreenLayout
import net.neednot.renttracker.feature.auth.presentation.uistate.SignInCodeUiState
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInCodeEvent
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInCodeViewModel
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignedInDestination
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_back
import net.neednot.renttracker.resources.auth_code_sent_to
import net.neednot.renttracker.resources.auth_code_title
import net.neednot.renttracker.resources.auth_send_new_code
import net.neednot.renttracker.resources.auth_send_new_code_in
import net.neednot.renttracker.resources.auth_verify_code
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
private fun SignInCodeContent(
    uiState: SignInCodeUiState,
    codeState: TextFieldState,
    onVerifyClicked: () -> Unit,
    onSendNewCodeClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canVerify = !uiState.isBusy && codeState.text.length == CODE_LENGTH
    AuthScreenLayout(modifier = modifier) {
        IconButton(onClick = onBackClicked) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.auth_back),
            )
        }
        Text(
            text = stringResource(Res.string.auth_code_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.auth_code_sent_to, uiState.email),
            style = MaterialTheme.typography.bodyLarge,
        )
        OtpField(
            state = codeState,
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.error != null,
            onDone = { if (canVerify) onVerifyClicked() },
        )
        uiState.error?.let { AuthErrorText(error = it) }
        Button(
            onClick = onVerifyClicked,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = canVerify,
        ) {
            if (uiState.isVerifying) AuthButtonProgress()
            Text(stringResource(Res.string.auth_verify_code))
        }
        TextButton(
            onClick = onSendNewCodeClicked,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = uiState.canSendNewCode,
        ) {
            if (uiState.isSendingNewCode) AuthButtonProgress()
            Text(
                if (uiState.secondsUntilNewCode > 0) {
                    stringResource(Res.string.auth_send_new_code_in, uiState.secondsUntilNewCode)
                } else {
                    stringResource(Res.string.auth_send_new_code)
                },
            )
        }
    }
}

@Preview
@Composable
private fun SignInCodeContentPreview() {
    RentTrackerTheme {
        SignInCodeContent(
            uiState = SignInCodeUiState(email = "dalton.h@gmail.com", secondsUntilNewCode = 42),
            codeState = rememberTextFieldState("123"),
            onVerifyClicked = {},
            onSendNewCodeClicked = {},
            onBackClicked = {},
        )
    }
}
