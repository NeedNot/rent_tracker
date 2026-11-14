package net.neednot.renttracker.feature.home.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.sync.Synced

interface HomeRepository {
    fun observeGroups(): Flow<Synced<List<GroupOverview>, HomeError>>

    suspend fun createGroup(name: String): Result<String, HomeError>
}
