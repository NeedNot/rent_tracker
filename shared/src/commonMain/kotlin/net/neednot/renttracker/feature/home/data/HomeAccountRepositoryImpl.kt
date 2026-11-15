package net.neednot.renttracker.feature.home.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.neednot.renttracker.core.data.AccountProfileSync
import net.neednot.renttracker.feature.home.domain.HomeAccountRepository

class HomeAccountRepositoryImpl(
    private val accountProfileSync: AccountProfileSync,
) : HomeAccountRepository {
    override fun observeAccountName(): Flow<String?> = accountProfileSync.observe().map { it.value?.displayName }
}
