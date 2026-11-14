package net.neednot.renttracker.feature.auth.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.domain.AuthError
import net.neednot.renttracker.feature.auth.domain.FakeAuthRepository
import net.neednot.renttracker.feature.auth.domain.SignedInAccount
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_error_google_failed
import net.neednot.renttracker.resources.auth_error_invalid_email
import net.neednot.renttracker.resources.auth_error_offline
import net.neednot.renttracker.resources.auth_error_send_rate_limited
import net.neednot.renttracker.resources.auth_error_send_rate_limited_minutes
import net.neednot.renttracker.resources.auth_error_send_rate_limited_seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest : MainDispatcherTest() {
    private val repository = FakeAuthRepository()
    private val viewModel by lazy { SignInViewModel(repository) }

    @Test
    fun emailsWithTheWrongShapeAreRejectedBeforeSending() =
        runTest {
            listOf("", "dalton", "dalton@", "@gmail.com", "dalton@gmail", "dal ton@gmail.com").forEach { email ->
                viewModel.onEmailChanged(email)
                viewModel.onSendCodeClicked()
                runCurrent()

                assertEquals(UiText.Resource(Res.string.auth_error_invalid_email), viewModel.uiState.value.emailError)
            }
            assertTrue(repository.sentCodesTo.isEmpty())
        }

    @Test
    fun editingTheEmailClearsItsError() =
        runTest {
            viewModel.onEmailChanged("dalton")
            viewModel.onSendCodeClicked()

            viewModel.onEmailChanged("dalton.h@gmail.com")

            assertNull(viewModel.uiState.value.emailError)
        }

    @Test
    fun sendingACodeOpensTheCodeScreenForTheTrimmedEmail() =
        runTest {
            viewModel.events.test {
                viewModel.onEmailChanged("  dalton.h@gmail.com ")
                viewModel.onSendCodeClicked()

                assertEquals(SignInEvent.CodeSent("dalton.h@gmail.com"), awaitItem())
            }
            assertEquals(listOf("dalton.h@gmail.com"), repository.sentCodesTo)
            assertFalse(viewModel.uiState.value.isSendingCode)
        }

    @Test
    fun showsBusyWhileTheCodeIsBeingSent() =
        runTest {
            viewModel.onEmailChanged("dalton.h@gmail.com")

            viewModel.onSendCodeClicked()

            assertTrue(viewModel.uiState.value.isSendingCode)
        }

    @Test
    fun sendFailuresExplainWhatToDo() =
        runTest {
            val expected =
                mapOf(
                    AuthError.Offline to UiText.Resource(Res.string.auth_error_offline),
                    AuthError.RateLimited(null) to UiText.Resource(Res.string.auth_error_send_rate_limited),
                    AuthError.RateLimited(45) to
                        UiText.Resource(Res.string.auth_error_send_rate_limited_seconds, listOf(45)),
                    AuthError.RateLimited(61) to
                        UiText.Resource(Res.string.auth_error_send_rate_limited_minutes, listOf(2)),
                )
            viewModel.onEmailChanged("dalton.h@gmail.com")

            expected.forEach { (error, text) ->
                repository.sendSignInCodeResult = Err(error)
                viewModel.onSendCodeClicked()
                runCurrent()

                assertEquals(text, viewModel.uiState.value.error)
            }
        }

    @Test
    fun firstGoogleSignInGoesToTheDisplayName() =
        runTest {
            repository.signInResult = Ok(SignedInAccount(displayName = "dalton.h", isNew = true))

            viewModel.events.test {
                viewModel.onGoogleSignInClicked()

                assertEquals(SignInEvent.SignedIn(SignedInDestination.DisplayName("dalton.h")), awaitItem())
            }
        }

    @Test
    fun laterGoogleSignInGoesHome() =
        runTest {
            repository.signInResult = Ok(SignedInAccount(displayName = "Dalton", isNew = false))

            viewModel.events.test {
                viewModel.onGoogleSignInClicked()

                assertEquals(SignInEvent.SignedIn(SignedInDestination.Home), awaitItem())
            }
        }

    @Test
    fun cancellingGoogleSignInShowsNoError() =
        runTest {
            repository.signInResult = Err(AuthError.GoogleSignInCancelled)

            viewModel.onGoogleSignInClicked()
            runCurrent()

            assertNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isSigningInWithGoogle)
        }

    @Test
    fun failedGoogleSignInSuggestsTheEmailCode() =
        runTest {
            repository.signInResult = Err(AuthError.GoogleSignInFailed)

            viewModel.onGoogleSignInClicked()
            runCurrent()

            assertEquals(UiText.Resource(Res.string.auth_error_google_failed), viewModel.uiState.value.error)
        }
}
