package net.neednot.renttracker.feature.auth.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.domain.AuthError
import net.neednot.renttracker.feature.auth.domain.FakeAuthRepository
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_error_display_name_length
import net.neednot.renttracker.resources.auth_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DisplayNameViewModelTest : MainDispatcherTest() {
    private val repository = FakeAuthRepository()
    private val viewModel by lazy { DisplayNameViewModel(defaultDisplayName = "dalton.h", authRepository = repository) }

    @Test
    fun startsWithTheDefaultName() =
        runTest {
            assertEquals("dalton.h", viewModel.uiState.value.displayName)
        }

    @Test
    fun continuingWithTheDefaultNameSavesNothing() =
        runTest {
            viewModel.events.test {
                viewModel.onContinueClicked()

                assertEquals(DisplayNameEvent.Done, awaitItem())
            }
            assertTrue(repository.savedDisplayNames.isEmpty())
        }

    @Test
    fun aChangedNameIsTrimmedAndSavedBeforeContinuing() =
        runTest {
            viewModel.events.test {
                viewModel.onDisplayNameChanged("  Dalton H ")
                viewModel.onContinueClicked()

                assertEquals(DisplayNameEvent.Done, awaitItem())
            }
            assertEquals(listOf("Dalton H"), repository.savedDisplayNames)
        }

    @Test
    fun blankAndOverlongNamesAreRejectedBeforeSaving() =
        runTest {
            listOf("", "   ", "x".repeat(101)).forEach { name ->
                viewModel.onDisplayNameChanged(name)
                viewModel.onContinueClicked()
                runCurrent()

                assertEquals(UiText.Resource(Res.string.auth_error_display_name_length), viewModel.uiState.value.error)
            }
            assertTrue(repository.savedDisplayNames.isEmpty())
        }

    @Test
    fun aHundredCharacterNameIsAccepted() =
        runTest {
            viewModel.onDisplayNameChanged("x".repeat(100))
            viewModel.onContinueClicked()
            runCurrent()

            assertNull(viewModel.uiState.value.error)
            assertEquals(listOf("x".repeat(100)), repository.savedDisplayNames)
        }

    @Test
    fun aFailedSaveStaysOnTheScreenWithTheReason() =
        runTest {
            repository.setDisplayNameResult = Err(AuthError.Offline)

            viewModel.events.test {
                viewModel.onDisplayNameChanged("Dalton")
                viewModel.onContinueClicked()
                runCurrent()

                expectNoEvents()
            }
            assertEquals(UiText.Resource(Res.string.auth_error_offline), viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun editingTheNameClearsTheError() =
        runTest {
            viewModel.onDisplayNameChanged("")
            viewModel.onContinueClicked()

            viewModel.onDisplayNameChanged("D")

            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun aNameOverAHundredCharactersIsFlaggedAsItIsTyped() =
        runTest {
            viewModel.onDisplayNameChanged("a".repeat(101))

            assertEquals(UiText.Resource(Res.string.auth_error_display_name_length), viewModel.uiState.value.error)
        }
}
