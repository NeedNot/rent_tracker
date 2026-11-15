package net.neednot.renttracker.feature.home.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.sync.Synced

interface HomeRepository {
    fun observeHome(): Flow<Synced<HomeOverview, HomeError>>

    fun observeArchivedGroups(): Flow<Synced<List<ArchivedGroup>, HomeError>>

    suspend fun createGroup(name: String): Result<String, HomeError>

    suspend fun acceptInvitation(inviteId: String): Result<Unit, HomeError>

    suspend fun declineInvitation(inviteId: String): Result<Unit, HomeError>
}
