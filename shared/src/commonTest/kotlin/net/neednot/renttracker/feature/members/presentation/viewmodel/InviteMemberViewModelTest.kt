package net.neednot.renttracker.feature.members.presentation.viewmodel

import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.domain.FakeMembersRepository
import net.neednot.renttracker.feature.members.domain.Member
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.MembersRepositoryCall
import net.neednot.renttracker.feature.members.domain.kevin
import net.neednot.renttracker.feature.members.domain.needNot
import net.neednot.renttracker.feature.members.domain.overviewSeenBy
import net.neednot.renttracker.feature.members.domain.synced
import net.neednot.renttracker.feature.members.presentation.uistate.EditOthersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_invite_error_email_format
import net.neednot.renttracker.resources.members_invite_error_email_required
import net.neednot.renttracker.resources.members_invite_sent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InviteMemberViewModelTest : MainDispatcherTest() {
    private val repository = FakeMembersRepository()
    private val messages = RecordingMessageDispatcher()

    private fun TestScope.openInvite(inviter: Member = needNot): InviteMemberViewModel {
        repository.members.value = synced(overviewSeenBy(inviter))
        val viewModel = InviteMemberViewModel("g1", repository, messages)
        runCurrent()
        return viewModel
    }

    @Test
    fun theOwnerIsOfferedEveryPresetAndLevelStartingAtBookkeeper() =
        runTest {
            val state = openInvite().uiState.value
            val form = state.form!!

            assertEquals(PermissionPreset.entries, form.presets)
            assertEquals(PermissionPreset.Bookkeeper, form.selectedPreset)
            assertEquals(AccessLevel.entries, form.levelRows.first { it.area == PermissionArea.Tenants }.levels)
            assertEquals(
                listOf(AccessLevel.Read, AccessLevel.Full),
                form.levelRows.first { it.area == PermissionArea.Settings }.levels,
            )
            assertEquals(EditOthersUiState(isOn = false, appliesNow = true, isEnabled = true), form.editOthers)
            assertNull(state.limitedInviterName)
        }

    @Test
    fun aLimitedInviterIsOfferedOnlyLevelsAtOrBelowTheirOwn() =
        runTest {
            val state = openInvite(inviter = kevin).uiState.value
            val form = state.form!!

            assertEquals("Kevin Cho", state.limitedInviterName)
            assertEquals(listOf(PermissionPreset.Viewer, PermissionPreset.Bookkeeper), form.presets)
            assertEquals(
                mapOf(
                    PermissionArea.Tenants to listOf(AccessLevel.Read),
                    PermissionArea.RentTerms to listOf(AccessLevel.Read),
                    PermissionArea.Payments to listOf(AccessLevel.Read, AccessLevel.AddEdit),
                    PermissionArea.Debts to listOf(AccessLevel.Read, AccessLevel.AddEdit),
                    PermissionArea.Members to listOf(AccessLevel.Read, AccessLevel.AddEdit),
                    PermissionArea.Settings to listOf(AccessLevel.Read),
                ),
                form.levelRows.associate { it.area to it.levels },
            )
            assertEquals(EditOthersUiState(isOn = false, appliesNow = true, isEnabled = false), form.editOthers)
        }

    @Test
    fun changingOneCategoryShowsCustomAndPickingAPresetFillsEveryCategory() =
        runTest {
            val viewModel = openInvite()

            viewModel.onLevelSelected(PermissionArea.Members, AccessLevel.AddEdit)
            assertNull(
                viewModel.uiState.value.form
                    ?.selectedPreset,
            )

            viewModel.onPresetClicked(PermissionPreset.Manager)
            assertEquals(
                PermissionPreset.Manager,
                viewModel.uiState.value.form
                    ?.selectedPreset,
            )
        }

    @Test
    fun leavingAddAndEditInPaymentsAndDebtsTurnsTheSwitchOffAndInactive() =
        runTest {
            val viewModel = openInvite()
            viewModel.onPresetClicked(PermissionPreset.Manager)

            viewModel.onLevelSelected(PermissionArea.Payments, AccessLevel.Read)
            viewModel.onLevelSelected(PermissionArea.Debts, AccessLevel.Full)

            assertEquals(
                EditOthersUiState(isOn = false, appliesNow = false, isEnabled = false),
                viewModel.uiState.value.form
                    ?.editOthers,
            )
        }

    @Test
    fun theEmailIsCheckedAsTypedAndRequiredOnlyAfterSend() =
        runTest {
            val viewModel = openInvite()

            viewModel.onEmailChanged("alex.m@example")
            assertEquals(
                UiText.Resource(Res.string.members_invite_error_email_format),
                viewModel.uiState.value.emailError,
            )

            viewModel.onEmailChanged("")
            assertNull(viewModel.uiState.value.emailError)

            viewModel.onSendClicked()
            assertEquals(
                UiText.Resource(Res.string.members_invite_error_email_required),
                viewModel.uiState.value.emailError,
            )
            assertEquals(emptyList(), repository.calls)
        }

    @Test
    fun sendingInvitesWithTheChosenPermissionsAnnouncesItAndCloses() =
        runTest {
            val viewModel = openInvite()
            val events = mutableListOf<InviteMemberEvent>()
            backgroundScope.launch { viewModel.events.collect { events += it } }
            viewModel.onEmailChanged(" alex.m@example.com ")

            viewModel.onSendClicked()
            assertTrue(viewModel.uiState.value.isSending)
            runCurrent()

            assertEquals(
                listOf<MembersRepositoryCall>(
                    MembersRepositoryCall.SendInvite(
                        "g1",
                        "alex.m@example.com",
                        PermissionPreset.Bookkeeper.permissions,
                    ),
                ),
                repository.calls,
            )
            assertEquals(
                listOf(UiMessage(UiText.Resource(Res.string.members_invite_sent, listOf("alex.m@example.com")))),
                messages.shown,
            )
            assertEquals(listOf<InviteMemberEvent>(InviteMemberEvent.InviteSent), events)
        }

    @Test
    fun theServersEmailErrorShowsUnderTheEmailField() =
        runTest {
            repository.result = Err(MembersError.ValidationFailed("Already a member of this group.", field = "email"))
            val viewModel = openInvite()
            viewModel.onEmailChanged("maria.lopez@example.com")

            viewModel.onSendClicked()
            runCurrent()

            assertEquals(UiText.Plain("Already a member of this group."), viewModel.uiState.value.emailError)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun aPermissionDeniedSendShowsTheServersMessageAtTheTopOfTheForm() =
        runTest {
            repository.result = Err(MembersError.PermissionDenied("You can only grant permissions you hold."))
            val viewModel = openInvite(inviter = kevin)
            viewModel.onEmailChanged("alex.m@example.com")

            viewModel.onSendClicked()
            runCurrent()

            assertEquals(UiText.Plain("You can only grant permissions you hold."), viewModel.uiState.value.error)
            assertEquals(false, viewModel.uiState.value.isSending)
        }
}
