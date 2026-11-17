package net.neednot.renttracker.feature.auth.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.AppMark
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.auth.presentation.components.AuthOrDivider
import net.neednot.renttracker.feature.auth.presentation.components.AuthScreenLayout
import net.neednot.renttracker.feature.auth.presentation.uistate.SignInUiState
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInEvent
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignInViewModel
import net.neednot.renttracker.feature.auth.presentation.viewmodel.SignedInDestination
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.app_name
import net.neednot.renttracker.resources.auth_email_label
import net.neednot.renttracker.resources.auth_send_code
import net.neednot.renttracker.resources.auth_sign_in_help
import net.neednot.renttracker.resources.auth_sign_in_offline
import net.neednot.renttracker.resources.auth_sign_in_subtitle
import net.neednot.renttracker.resources.auth_sign_in_with_google
import net.neednot.renttracker.resources.ic_account_circle
import net.neednot.renttracker.resources.ic_cloud_off
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
internal fun SignInContent(
    uiState: SignInUiState,
    onEmailChanged: (String) -> Unit,
    onSendCodeClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenLayout(modifier = modifier, offlineBanner = uiState.offlineBanner?.asString()) {
        SignInIntro(modifier = Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (uiState.offlineBanner != null) {
                Banner(
                    text = stringResource(Res.string.auth_sign_in_offline),
                    kind = BannerKind.Info,
                    icon = Res.drawable.ic_cloud_off,
                )
            } else {
                SignInActions(
                    uiState = uiState,
                    onEmailChanged = onEmailChanged,
                    onSendCodeClicked = onSendCodeClicked,
                    onGoogleSignInClicked = onGoogleSignInClicked,
                )
            }
        }
    }
}

@Composable
private fun SignInIntro(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ContentLayout.Auth.spacing, Alignment.CenterVertically),
    ) {
        AppMark()
        Text(
            text = stringResource(Res.string.app_name),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.auth_sign_in_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SignInActions(
    uiState: SignInUiState,
    onEmailChanged: (String) -> Unit,
    onSendCodeClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
) {
    uiState.error?.let { Banner(text = it.asString(), kind = BannerKind.Error) }
    RtButton(
        text = stringResource(Res.string.auth_sign_in_with_google),
        onClick = onGoogleSignInClicked,
        modifier = Modifier.fillMaxWidth(),
        variant = ButtonVariant.Outlined,
        enabled = !uiState.isSendingCode,
        isWorking = uiState.isSigningInWithGoogle,
        leadingIcon = Res.drawable.ic_account_circle,
    )
    AuthOrDivider()
    FormTextField(
        label = stringResource(Res.string.auth_email_label),
        text = uiState.email,
        onTextChanged = onEmailChanged,
        error = uiState.emailError,
        enabled = !uiState.isBusy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSendCodeClicked() }),
        contentType = ContentType.EmailAddress,
    )
    RtButton(
        text = stringResource(Res.string.auth_send_code),
        onClick = onSendCodeClicked,
        modifier = Modifier.fillMaxWidth(),
        enabled = !uiState.isSigningInWithGoogle,
        isWorking = uiState.isSendingCode,
    )
    Text(
        text = stringResource(Res.string.auth_sign_in_help),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Preview
@Composable
private fun SignInContentPreview() {
    RentTrackerTheme {
        SignInContent(
            uiState = SignInUiState(email = "you@example.com"),
            onEmailChanged = {},
            onSendCodeClicked = {},
            onGoogleSignInClicked = {},
        )
    }
}
