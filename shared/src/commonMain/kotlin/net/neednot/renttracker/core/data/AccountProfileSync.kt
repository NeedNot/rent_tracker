package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.sync.syncedFlow
import net.neednot.renttracker.model.accounts.AccountsMeResult

class AccountProfileSync(
    private val backend: Backend,
    database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
) {
    private val accountProfileDao = database.accountProfileDao()

    fun observe(): Flow<Synced<AccountProfileEntity?, BackendError>> =
        syncedFlow(
            cached = accountProfileDao.observe(),
            serverResults = backend.subscribe(ACCOUNT_ME, JsonObject(emptyMap()), AccountsMeResult.serializer()),
            store = { result -> screenResultStore.replaceAccountProfile(result.toEntity()) },
        )

    suspend fun clear() {
        accountProfileDao.deleteAll()
    }

    private companion object {
        const val ACCOUNT_ME = "accounts:me"
    }
}
