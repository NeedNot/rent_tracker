package net.neednot.renttracker.feature.home.domain

import kotlinx.coroutines.flow.MutableStateFlow

class FakeHomeAccountRepository : HomeAccountRepository {
    val accountName = MutableStateFlow<String?>(null)

    override fun observeAccountName() = accountName
}
