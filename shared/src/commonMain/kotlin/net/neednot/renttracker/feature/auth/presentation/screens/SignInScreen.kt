package net.neednot.renttracker.feature.auth.presentation.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.auth.presentation.components.AuthButtonProgress
import net.neednot.renttracker.feature.auth.presentation.components.AuthErrorText
import net.neednot.renttracker.feature.auth.presentation.components.AuthScreenLayout
import net.neednot.renttracker.feature.auth.presentation.uistate.SignInUiState
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInEvent
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInViewModel
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignedInDestination
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_email_label
import net.neednot.renttracker.resources.auth_or_divider
import net.neednot.renttracker.resources.auth_send_code
import net.neednot.renttracker.resources.auth_sign_in_subtitle
import net.neednot.renttracker.resources.auth_sign_in_title
import net.neednot.renttracker.resources.auth_sign_in_with_google
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInScreen(
    onCodeSent: (email: String) -> Unit,
    onSignedIn: (SignedInDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SignInEvent.CodeSent -> onCodeSent(event.email)
                is SignInEvent.SignedIn -> onSignedIn(event.destination)
            }
        }
    }
    SignInContent(
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onSendCodeClicked = viewModel::onSendCodeClicked,
        onGoogleSignInClicked = viewModel::onGoogleSignInClicked,
        modifier = modifier,
    )
}

@Composable
private fun SignInContent(
    uiState: SignInUiState,
    onEmailChanged: (String) -> Unit,
    onSendCodeClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenLayout(modifier = modifier) {
        Text(
            text = stringResource(Res.string.auth_sign_in_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.auth_sign_in_subtitle),
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress },
            enabled = !uiState.isBusy,
            label = { Text(stringResource(Res.string.auth_email_label)) },
            supportingText = uiState.emailError?.let { error -> { Text(error.asString()) } },
            isError = uiState.emailError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendCodeClicked() }),
        )
        uiState.error?.let { AuthErrorText(error = it) }
        Button(
            onClick = onSendCodeClicked,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !uiState.isBusy,
        ) {
            if (uiState.isSendingCode) AuthButtonProgress()
            Text(stringResource(Res.string.auth_send_code))
        }
        Text(
            text = stringResource(Res.string.auth_or_divider),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = onGoogleSignInClicked,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !uiState.isBusy,
        ) {
            if (uiState.isSigningInWithGoogle) AuthButtonProgress()
            Text(stringResource(Res.string.auth_sign_in_with_google))
        }
    }
}

@Preview
@Composable
private fun SignInContentPreview() {
    RentTrackerTheme {
        SignInContent(
            uiState = SignInUiState(email = "dalton.h@gmail.com"),
            onEmailChanged = {},
            onSendCodeClicked = {},
            onGoogleSignInClicked = {},
        )
    }
}
