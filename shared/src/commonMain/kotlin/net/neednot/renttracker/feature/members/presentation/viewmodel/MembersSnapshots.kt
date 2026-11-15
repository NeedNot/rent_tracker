package net.neednot.renttracker.feature.members.presentation.viewmodel

import kotlinx.coroutines.flow.first
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.MembersOverview
import net.neednot.renttracker.feature.members.domain.MembersRepository
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_error_unexpected

internal suspend fun MembersRepository.cachedOrFirstSyncedMembers(
    groupId: String,
): Synced<MembersOverview?, MembersError> =
    observeMembers(groupId).first { it.value != null || it.status != SyncStatus.Syncing }

internal suspend fun MembersRepository.membersFromServer(groupId: String): Synced<MembersOverview?, MembersError> =
    observeMembers(groupId).first { it.status != SyncStatus.Syncing }

internal fun Synced<MembersOverview?, MembersError>.syncErrorText(): UiText? =
    (status as? SyncStatus.Failed)?.error?.text()

internal fun Synced<MembersOverview?, MembersError>.loadErrorText(): UiText =
    syncErrorText() ?: UiText.Resource(Res.string.members_error_unexpected)
