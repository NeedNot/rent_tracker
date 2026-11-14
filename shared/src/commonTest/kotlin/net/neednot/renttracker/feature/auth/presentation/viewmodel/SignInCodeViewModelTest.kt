package net.neednot.renttracker.feature.auth.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.domain.AuthError
import net.neednot.renttracker.feature.auth.domain.FakeAuthRepository
import net.neednot.renttracker.feature.auth.domain.SignedInAccount
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_error_code_expired
import net.neednot.renttracker.resources.auth_error_code_incomplete
import net.neednot.renttracker.resources.auth_error_code_rate_limited
import net.neednot.renttracker.resources.auth_error_code_rate_limited_seconds
import net.neednot.renttracker.resources.auth_error_invalid_code
import net.neednot.renttracker.resources.auth_error_offline
import net.neednot.renttracker.resources.auth_error_send_rate_limited_minutes
import net.neednot.renttracker.resources.auth_error_too_many_attempts
import net.neednot.renttracker.resources.auth_error_unexpected
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SignInCodeViewModelTest : MainDispatcherTest() {
    private val repository = FakeAuthRepository()
    private val viewModel by lazy { SignInCodeViewModel(email = EMAIL, authRepository = repository) }

    @Test
    fun showsWhichEmailTheCodeWentTo() =
        runTest {
            assertEquals(EMAIL, viewModel.uiState.value.email)
        }

    @Test
    fun firstSignInGoesToTheDisplayName() =
        runTest {
            repository.signInResult = Ok(SignedInAccount(displayName = "dalton.h", isNew = true))

            viewModel.events.test {
                viewModel.onVerifyClicked("123456")

                assertEquals(SignInCodeEvent.SignedIn(SignedInDestination.DisplayName("dalton.h")), awaitItem())
            }
            assertEquals(listOf(EMAIL to "123456"), repository.codeSignIns)
        }

    @Test
    fun laterSignInGoesHome() =
        runTest {
            repository.signInResult = Ok(SignedInAccount(displayName = "Dalton", isNew = false))

            viewModel.events.test {
                viewModel.onVerifyClicked("123456")

                assertEquals(SignInCodeEvent.SignedIn(SignedInDestination.Home), awaitItem())
            }
        }

    @Test
    fun codesThatAreNotSixDigitsAreNotSent() =
        runTest {
            listOf("", "12345", "1234567", "12a456").forEach { code ->
                viewModel.onVerifyClicked(code)
                runCurrent()

                assertEquals(UiText.Resource(Res.string.auth_error_code_incomplete), viewModel.uiState.value.error)
            }
            assertTrue(repository.codeSignIns.isEmpty())
        }

    @Test
    fun codeFailuresExplainWhatToDo() =
        runTest {
            val expected =
                mapOf(
                    AuthError.InvalidCode to UiText.Resource(Res.string.auth_error_invalid_code),
                    AuthError.CodeExpired to UiText.Resource(Res.string.auth_error_code_expired),
                    AuthError.TooManyAttempts to UiText.Resource(Res.string.auth_error_too_many_attempts),
                    AuthError.RateLimited(null) to UiText.Resource(Res.string.auth_error_code_rate_limited),
                    AuthError.RateLimited(30) to
                        UiText.Resource(Res.string.auth_error_code_rate_limited_seconds, listOf(30)),
                    AuthError.Offline to UiText.Resource(Res.string.auth_error_offline),
                    AuthError.Unexpected("bug") to UiText.Resource(Res.string.auth_error_unexpected),
                )

            expected.forEach { (error, text) ->
                repository.signInResult = Err(error)
                viewModel.onVerifyClicked("123456")
                runCurrent()

                assertEquals(text, viewModel.uiState.value.error)
                assertFalse(viewModel.uiState.value.isVerifying)
            }
        }

    @Test
    fun newCodeIsLockedForSixtySecondsAfterTheFirstSend() =
        runTest {
            runCurrent()
            assertEquals(60, viewModel.uiState.value.secondsUntilNewCode)
            assertFalse(viewModel.uiState.value.canSendNewCode)

            advanceTimeBy(59.seconds)
            runCurrent()
            assertEquals(1, viewModel.uiState.value.secondsUntilNewCode)
            assertFalse(viewModel.uiState.value.canSendNewCode)

            advanceTimeBy(1.seconds)
            runCurrent()
            assertEquals(0, viewModel.uiState.value.secondsUntilNewCode)
            assertTrue(viewModel.uiState.value.canSendNewCode)
        }

    @Test
    fun askingForANewCodeTooEarlyDoesNothing() =
        runTest {
            viewModel.onSendNewCodeClicked()
            runCurrent()

            assertTrue(repository.sentCodesTo.isEmpty())
        }

    @Test
    fun sendingANewCodeStartsTheSixtySecondWaitAgain() =
        runTest {
            waitUntilNewCodeIsAllowed()

            viewModel.onSendNewCodeClicked()
            runCurrent()

            assertEquals(listOf(EMAIL), repository.sentCodesTo)
            assertEquals(60, viewModel.uiState.value.secondsUntilNewCode)
            assertFalse(viewModel.uiState.value.canSendNewCode)
        }

    @Test
    fun rateLimitedNewCodeWaitsAsLongAsTheServerSays() =
        runTest {
            repository.sendSignInCodeResult = Err(AuthError.RateLimited(retryAfterSeconds = 1800))
            waitUntilNewCodeIsAllowed()

            viewModel.onSendNewCodeClicked()
            runCurrent()

            assertEquals(
                UiText.Resource(Res.string.auth_error_send_rate_limited_minutes, listOf(30)),
                viewModel.uiState.value.error,
            )
            assertEquals(1800, viewModel.uiState.value.secondsUntilNewCode)
        }

    @Test
    fun newCodeThatCouldNotBeSentCanBeTriedAgainAtOnce() =
        runTest {
            repository.sendSignInCodeResult = Err(AuthError.Offline)
            waitUntilNewCodeIsAllowed()

            viewModel.onSendNewCodeClicked()
            runCurrent()

            assertEquals(UiText.Resource(Res.string.auth_error_offline), viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.canSendNewCode)
        }

    private fun TestScope.waitUntilNewCodeIsAllowed() {
        viewModel
        advanceTimeBy(60.seconds)
        runCurrent()
    }

    private companion object {
        const val EMAIL = "dalton.h@gmail.com"
    }
}
