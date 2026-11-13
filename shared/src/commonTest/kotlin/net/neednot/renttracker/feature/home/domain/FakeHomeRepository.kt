package net.neednot.renttracker.feature.home.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

class FakeHomeRepository : HomeRepository {
    val groups = MutableStateFlow<Synced<List<GroupOverview>, HomeError>>(Synced(emptyList(), SyncStatus.Syncing))

    var createGroupResult: Result<String, HomeError> = Ok("g1")
    val createdGroupNames = mutableListOf<String>()

    override fun observeGroups() = groups

    override suspend fun createGroup(name: String): Result<String, HomeError> {
        createdGroupNames += name
        return createGroupResult
    }
}
