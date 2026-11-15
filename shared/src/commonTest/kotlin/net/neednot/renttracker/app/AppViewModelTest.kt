package net.neednot.renttracker.app

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.core.data.AccessLossReason
import net.neednot.renttracker.core.data.LostGroupAccess
import net.neednot.renttracker.core.data.PendingPaymentLink
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.auth.domain.FakeAuthRepository
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_error_not_found
import net.neednot.renttracker.resources.lost_access_removed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest : MainDispatcherTest() {
    private val repository = FakeAuthRepository()
    private val lostGroupAccess = MutableSharedFlow<LostGroupAccess>()
    private val messageDispatcher = RecordingMessageDispatcher()
    private val pendingPaymentLinks = MutableSharedFlow<PendingPaymentLink>()
    private var pendingSyncRunning = false
    private val viewModel by lazy {
        AppViewModel(
            repository,
            lostGroupAccess,
            ::syncPendingPaymentsWhileOnline,
            pendingPaymentLinks,
            messageDispatcher,
        )
    }

    private suspend fun syncPendingPaymentsWhileOnline() {
        pendingSyncRunning = true
        awaitCancellation()
    }

    @Test
    fun aSyncNotificationOpensThePendingPaymentOfItsTenant() =
        runTest {
            val events = viewModel.events
            runCurrent()

            events.test {
                pendingPaymentLinks.emit(PendingPaymentLink("t1", "c1"))

                assertEquals(AppEvent.OpenPendingPayment("t1", "c1"), awaitItem())
            }
        }

    @Test
    fun syncsPendingPaymentsWhileTheAppIsOpen() =
        runTest {
            viewModel
            runCurrent()

            assertTrue(pendingSyncRunning)
        }

    @Test
    fun followsTheSessionFromLoadingToSignedInToSignedOut() =
        runTest {
            repository.sessionState.value = AuthSessionState.Loading

            viewModel.sessionState.test {
                assertEquals(AuthSessionState.Loading, awaitItem())

                repository.sessionState.value = AuthSessionState.SignedIn("user-1")
                assertEquals(AuthSessionState.SignedIn("user-1"), awaitItem())

                repository.sessionState.value = AuthSessionState.SignedOut
                assertEquals(AuthSessionState.SignedOut, awaitItem())
            }
        }

    @Test
    fun aRemovedMemberReturnsHomeAndIsToldWhichGroupTheyLost() =
        runTest {
            val events = viewModel.events
            runCurrent()

            events.test {
                lostGroupAccess.emit(LostGroupAccess("g1", "1st Street", AccessLossReason.Removed))

                assertEquals(AppEvent.ReturnHome, awaitItem())
            }
            assertEquals(
                listOf(UiMessage(UiText.Resource(Res.string.lost_access_removed, listOf("1st Street")))),
                messageDispatcher.shown,
            )
        }

    @Test
    fun aGroupThatIsGoneReturnsHomeWithTheGroupGoneError() =
        runTest {
            val events = viewModel.events
            runCurrent()

            events.test {
                lostGroupAccess.emit(LostGroupAccess("g1", "1st Street", AccessLossReason.GroupGone))

                assertEquals(AppEvent.ReturnHome, awaitItem())
            }
            assertEquals(
                listOf(UiMessage(UiText.Resource(Res.string.group_error_not_found), UiMessageVariant.Error)),
                messageDispatcher.shown,
            )
        }
}
