package net.neednot.renttracker.feature.members.presentation.viewmodel

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.ui.firstNameOf
import net.neednot.renttracker.feature.members.domain.Member
import net.neednot.renttracker.feature.members.domain.MembersRepository
import net.neednot.renttracker.feature.members.presentation.uistate.RemoveMemberDialogUiState

internal class RemoveMemberFlow(
    private val scope: CoroutineScope,
    private val membersRepository: MembersRepository,
    private val onRemoved: suspend () -> Unit = {},
) {
    private val mutableDialog = MutableStateFlow<RemoveMemberDialogUiState?>(null)
    val dialog: StateFlow<RemoveMemberDialogUiState?> = mutableDialog.asStateFlow()

    fun open(
        member: Member,
        groupName: String,
    ) {
        mutableDialog.value =
            RemoveMemberDialogUiState(
                membershipId = member.membershipId,
                memberName = member.displayName,
                memberFirstName = firstNameOf(member.displayName),
                groupName = groupName,
            )
    }

    fun dismiss() {
        if (mutableDialog.value?.isWorking != true) mutableDialog.value = null
    }

    fun confirm() {
        val opened = mutableDialog.value
        if (opened == null || opened.isWorking) return
        mutableDialog.value = opened.copy(isWorking = true, error = null)
        scope.launch {
            membersRepository
                .removeMember(opened.membershipId)
                .onOk {
                    mutableDialog.value = null
                    onRemoved()
                }.onErr { error -> mutableDialog.value = opened.copy(isWorking = false, error = error.text()) }
        }
    }
}
