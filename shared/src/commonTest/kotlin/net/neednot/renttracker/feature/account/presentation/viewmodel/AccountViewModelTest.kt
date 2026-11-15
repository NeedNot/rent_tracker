package net.neednot.renttracker.feature.account.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.FakeAccountRepository
import net.neednot.renttracker.feature.account.presentation.uistate.AccountUiState
import net.neednot.renttracker.feature.account.presentation.uistate.DeletionRowUiState
import net.neednot.renttracker.feature.account.presentation.uistate.EditNameDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_deletion_requested_detail
import net.neednot.renttracker.resources.account_error_name_length
import net.neednot.renttracker.resources.account_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest : MainDispatcherTest() {
    private val repository = FakeAccountRepository()
    private val messages = RecordingMessageDispatcher()
    private val viewModel by lazy { AccountViewModel(repository, messages, TimeZone.of("America/Chicago")) }

    private suspend fun app.cash.turbine.ReceiveTurbine<AccountUiState>.awaitContent(): AccountUiState.Content =
        awaitItem() as AccountUiState.Content

    @Test
    fun showsLoadingUntilTheProfileArrives() =
        runTest {
            viewModel.uiState.test {
                assertEquals(AccountUiState.Loading, awaitItem())
            }
        }

    @Test
    fun showsTheProfileWithInitialsAndAvailableDeletion() =
        runTest {
            repository.showProfile()

            viewModel.uiState.test {
                skipItems(1)
                assertEquals(
                    AccountUiState.Content(
                        initials = "SR",
                        displayName = "Sam Rivera",
                        email = "sam@example.com",
                        deletion = DeletionRowUiState.Available,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun showsALoadErrorWhenNothingIsCached() =
        runTest {
            repository.profile.value = Synced(null, SyncStatus.Failed(AccountError.Offline))

            viewModel.uiState.test {
                skipItems(1)
                assertEquals(AccountUiState.Error(UiText.Resource(Res.string.account_error_offline)), awaitItem())
            }
        }

    @Test
    fun showsTheRequestedDeletionDateInThePhonesTimeZone() =
        runTest {
            repository.showProfile(deletionRequestedAt = Instant.parse("2026-11-15T03:00:00Z"))

            viewModel.uiState.test {
                skipItems(1)
                assertEquals(
                    DeletionRowUiState.Requested(
                        UiText.Resource(Res.string.account_deletion_requested_detail, listOf("Nov 14")),
                    ),
                    awaitContent().deletion,
                )
            }
        }

    @Test
    fun editNameOpensTheDialogPrefilledWithTheCurrentName() =
        runTest {
            repository.showProfile()

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()

                viewModel.onEditNameClicked()

                assertEquals(EditNameDialogUiState(name = "Sam Rivera"), awaitContent().editNameDialog)
            }
        }

    @Test
    fun savingANewNameSendsItTrimmedAndClosesTheDialog() =
        runTest {
            repository.showProfile()

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()
                viewModel.onEditNameClicked()
                awaitItem()

                viewModel.onNameChanged("  Sam R  ")
                awaitItem()
                viewModel.onEditNameSaved()

                assertEquals(true, awaitContent().editNameDialog?.isSaving)
                assertEquals(null, awaitContent().editNameDialog)
                assertEquals(listOf("Sam R"), repository.editedNames)
            }
        }

    @Test
    fun aBlankNameShowsTheLengthErrorWithoutCallingTheServer() =
        runTest {
            repository.showProfile()

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()
                viewModel.onEditNameClicked()
                awaitItem()
                viewModel.onNameChanged("   ")
                awaitItem()

                viewModel.onEditNameSaved()

                assertEquals(
                    UiText.Resource(Res.string.account_error_name_length),
                    awaitContent().editNameDialog?.error,
                )
                assertEquals(emptyList(), repository.editedNames)
            }
        }

    @Test
    fun aNameOverOneHundredCharactersShowsTheLengthErrorWhileTyping() =
        runTest {
            repository.showProfile()

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()
                viewModel.onEditNameClicked()
                awaitItem()

                viewModel.onNameChanged("a".repeat(101))

                assertEquals(
                    UiText.Resource(Res.string.account_error_name_length),
                    awaitContent().editNameDialog?.error,
                )
            }
        }

    @Test
    fun aServerValidationErrorShowsUnderTheFieldAndKeepsTheDialogOpen() =
        runTest {
            repository.showProfile()
            repository.editNameResult = Err(AccountError.ValidationFailed("Name is not allowed.", "displayName"))

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()
                viewModel.onEditNameClicked()
                awaitItem()

                viewModel.onEditNameSaved()
                awaitItem()

                assertEquals(
                    EditNameDialogUiState(name = "Sam Rivera", error = UiText.Plain("Name is not allowed.")),
                    awaitContent().editNameDialog,
                )
            }
        }

    @Test
    fun anOfflineSaveShowsASnackbarAndKeepsTheDialogOpen() =
        runTest {
            repository.showProfile()
            repository.editNameResult = Err(AccountError.Offline)
            viewModel.uiState.test {
                skipItems(1)
                awaitItem()
                viewModel.onEditNameClicked()
                awaitItem()

                viewModel.onEditNameSaved()
                awaitItem()
                runCurrent()

                assertEquals(EditNameDialogUiState(name = "Sam Rivera"), awaitContent().editNameDialog)
                assertEquals(
                    listOf(UiMessage(UiText.Resource(Res.string.account_error_offline), UiMessageVariant.Error)),
                    messages.shown,
                )
            }
        }

    @Test
    fun requestingDeletionAsksForConfirmationFirst() =
        runTest {
            repository.showProfile()

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()

                viewModel.onRequestDeletionClicked()

                assertEquals(true, awaitContent().isDeleteDialogShown)
                assertEquals(0, repository.deletionRequestCount)
            }
        }

    @Test
    fun cancellingTheDeletionDialogRequestsNothing() =
        runTest {
            repository.showProfile()

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()
                viewModel.onRequestDeletionClicked()
                awaitItem()

                viewModel.onRequestDeletionDismissed()

                assertEquals(false, awaitContent().isDeleteDialogShown)
                assertEquals(0, repository.deletionRequestCount)
            }
        }

    @Test
    fun confirmingDeletionRequestsItAndTheRowBecomesRequested() =
        runTest {
            repository.showProfile()

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()
                viewModel.onRequestDeletionClicked()
                awaitItem()

                viewModel.onRequestDeletionConfirmed()
                awaitItem()
                runCurrent()
                repository.showProfile(deletionRequestedAt = Instant.parse("2026-11-14T18:00:00Z"))

                assertEquals(1, repository.deletionRequestCount)
                assertEquals(
                    DeletionRowUiState.Requested(
                        UiText.Resource(Res.string.account_deletion_requested_detail, listOf("Nov 14")),
                    ),
                    awaitContent().deletion,
                )
            }
        }

    @Test
    fun aDeletionRefusedForOwnedGroupsShowsTheServerMessageInASnackbar() =
        runTest {
            val message = "Transfer ownership of 1st Street first."
            repository.showProfile()
            repository.requestDeletionResult = Err(AccountError.ValidationFailed(message))

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()

                viewModel.onRequestDeletionConfirmed()
                runCurrent()

                assertEquals(
                    listOf(UiMessage(UiText.Plain(message), UiMessageVariant.Error)),
                    messages.shown,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun anOfflineDeletionRequestShowsTheOfflineMessage() =
        runTest {
            repository.showProfile()
            repository.requestDeletionResult = Err(AccountError.Offline)

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()

                viewModel.onRequestDeletionConfirmed()
                runCurrent()

                assertEquals(
                    listOf(UiMessage(UiText.Resource(Res.string.account_error_offline), UiMessageVariant.Error)),
                    messages.shown,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun signingOutEndsTheSessionImmediately() =
        runTest {
            repository.showProfile()

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()

                viewModel.onSignOutClicked()
                runCurrent()

                assertEquals(1, repository.signOutCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aFailedSignOutShowsASnackbar() =
        runTest {
            repository.showProfile()
            repository.signOutResult = Err(AccountError.Offline)

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()

                viewModel.onSignOutClicked()
                runCurrent()

                assertEquals(
                    listOf(UiMessage(UiText.Resource(Res.string.account_error_offline), UiMessageVariant.Error)),
                    messages.shown,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
}
