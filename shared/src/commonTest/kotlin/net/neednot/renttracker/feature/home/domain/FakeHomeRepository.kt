package net.neednot.renttracker.feature.home.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

class FakeHomeRepository : HomeRepository {
    val home =
        MutableStateFlow<Synced<HomeOverview, HomeError>>(
            Synced(HomeOverview(emptyList(), emptyList(), archivedGroupCount = 0), SyncStatus.Syncing),
        )
    val archivedGroups =
        MutableStateFlow<Synced<List<ArchivedGroup>, HomeError>>(Synced(emptyList(), SyncStatus.Syncing))

    var createGroupResult: Result<String, HomeError> = Ok("g1")
    var invitationResult: Result<Unit, HomeError> = Ok(Unit)
    val createdGroupNames = mutableListOf<String>()
    val acceptedInviteIds = mutableListOf<String>()
    val declinedInviteIds = mutableListOf<String>()

    fun showGroups(
        groups: List<GroupOverview>,
        status: SyncStatus<HomeError> = SyncStatus.Synced,
    ) {
        home.value = Synced(home.value.value.copy(groups = groups), status)
    }

    override fun observeHome() = home

    override fun observeArchivedGroups() = archivedGroups

    override suspend fun createGroup(name: String): Result<String, HomeError> {
        createdGroupNames += name
        return createGroupResult
    }

    override suspend fun acceptInvitation(inviteId: String): Result<Unit, HomeError> {
        acceptedInviteIds += inviteId
        return invitationResult
    }

    override suspend fun declineInvitation(inviteId: String): Result<Unit, HomeError> {
        declinedInviteIds += inviteId
        return invitationResult
    }
}
