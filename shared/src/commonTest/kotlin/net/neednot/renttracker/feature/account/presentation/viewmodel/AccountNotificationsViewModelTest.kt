package net.neednot.renttracker.feature.account.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.FakeNotificationSettingsRepository
import net.neednot.renttracker.feature.account.domain.GroupNotificationSetting
import net.neednot.renttracker.feature.account.presentation.uistate.GroupNotificationsRowUiState
import net.neednot.renttracker.feature.account.presentation.uistate.NotificationsSectionUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AccountNotificationsViewModelTest : MainDispatcherTest() {
    private val repository = FakeNotificationSettingsRepository()
    private val messages = RecordingMessageDispatcher()
    private val viewModel by lazy { AccountNotificationsViewModel(repository, messages) }

    private val firstStreet = GroupNotificationSetting("g1", "1st Street", notificationsOn = true)

    private fun section(
        isOn: Boolean,
        isBlockedOnPhone: Boolean = false,
    ) = NotificationsSectionUiState.Shown(
        groups = listOf(GroupNotificationsRowUiState("g1", "1st Street", isOn)),
        isBlockedOnPhone = isBlockedOnPhone,
    )

    @Test
    fun staysHiddenUntilTheServerAnswers() =
        runTest {
            viewModel.uiState.test {
                assertEquals(NotificationsSectionUiState.Hidden, awaitItem())
            }
        }

    @Test
    fun showsASwitchPerGroup() =
        runTest {
            repository.blockedOnPhone = true
            viewModel.uiState.test {
                skipItems(1)

                repository.settings.value = Ok(listOf(firstStreet))

                assertEquals(section(isOn = true, isBlockedOnPhone = true), awaitItem())
            }
        }

    @Test
    fun aToggleShowsAtOnceAndSavesTheNewValue() =
        runTest {
            repository.saveAnswer = CompletableDeferred()
            repository.settings.value = Ok(listOf(firstStreet))
            viewModel.uiState.test {
                skipItems(2)

                viewModel.onGroupToggled("g1")

                assertEquals(section(isOn = false), awaitItem())
                assertEquals(listOf("g1" to false), repository.saved)
            }
        }

    @Test
    fun aRefusedToggleSwitchesBackAndSaysWhy() =
        runTest {
            repository.saveAnswer = CompletableDeferred()
            repository.settings.value = Ok(listOf(firstStreet))
            viewModel.uiState.test {
                skipItems(2)
                viewModel.onGroupToggled("g1")
                skipItems(1)

                repository.saveAnswer.complete(Err(AccountError.Offline))

                assertEquals(section(isOn = true), awaitItem())
            }
            assertEquals(
                listOf(UiMessage(UiText.Resource(Res.string.account_error_offline), UiMessageVariant.Error)),
                messages.shown,
            )
        }

    @Test
    fun aFailedSubscriptionHidesTheSection() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                repository.settings.value = Ok(listOf(firstStreet))
                skipItems(1)

                repository.settings.value = Err(AccountError.Unexpected("boom"))

                assertEquals(NotificationsSectionUiState.Hidden, awaitItem())
            }
        }
}
