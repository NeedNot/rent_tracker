package net.neednot.renttracker.feature.home.domain

import kotlinx.coroutines.flow.Flow

interface HomeAccountRepository {
    fun observeAccountName(): Flow<String?>
}
