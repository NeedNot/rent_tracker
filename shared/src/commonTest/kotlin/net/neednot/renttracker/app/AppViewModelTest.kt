package net.neednot.renttracker.app

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.feature.auth.domain.FakeAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class AppViewModelTest {
    @Test
    fun followsTheSessionFromLoadingToSignedInToSignedOut() =
        runTest {
            val repository = FakeAuthRepository()
            repository.sessionState.value = AuthSessionState.Loading
            val viewModel = AppViewModel(repository)

            viewModel.sessionState.test {
                assertEquals(AuthSessionState.Loading, awaitItem())

                repository.sessionState.value = AuthSessionState.SignedIn("user-1")
                assertEquals(AuthSessionState.SignedIn("user-1"), awaitItem())

                repository.sessionState.value = AuthSessionState.SignedOut
                assertEquals(AuthSessionState.SignedOut, awaitItem())
            }
        }
}
