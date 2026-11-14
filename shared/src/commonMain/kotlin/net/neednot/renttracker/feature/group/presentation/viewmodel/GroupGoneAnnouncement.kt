package net.neednot.renttracker.feature.group.presentation.viewmodel

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.domain.GroupError

internal fun Flow<Synced<GroupDetails?, GroupError>>.announcingGroupGone(
    messageDispatcher: MessageDispatcher,
    events: SendChannel<GroupEvent>,
): Flow<Synced<GroupDetails?, GroupError>> =
    onEach { group ->
        if ((group.status as? SyncStatus.Failed)?.error != GroupError.NotFound) return@onEach
        messageDispatcher.show(UiMessage(text = GroupError.NotFound.text(), variant = UiMessageVariant.Error))
        events.send(GroupEvent.GroupGone)
    }
