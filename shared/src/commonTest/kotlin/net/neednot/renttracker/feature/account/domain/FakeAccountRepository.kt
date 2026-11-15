package net.neednot.renttracker.feature.account.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import kotlin.time.Instant

class FakeAccountRepository : AccountRepository {
    val profile = MutableStateFlow<Synced<AccountProfile?, AccountError>>(Synced(null, SyncStatus.Syncing))

    var editNameResult: Result<Unit, AccountError> = Ok(Unit)
    var requestDeletionResult: Result<Unit, AccountError> = Ok(Unit)
    var signOutResult: Result<Unit, AccountError> = Ok(Unit)

    val editedNames = mutableListOf<String>()
    var deletionRequestCount = 0
    var signOutCount = 0

    fun showProfile(
        displayName: String = "Sam Rivera",
        deletionRequestedAt: Instant? = null,
    ) {
        profile.value = Synced(AccountProfile(displayName, "sam@example.com", deletionRequestedAt), SyncStatus.Synced)
    }

    override fun observeProfile() = profile

    override suspend fun editName(displayName: String): Result<Unit, AccountError> {
        editedNames += displayName
        return editNameResult
    }

    override suspend fun requestDeletion(): Result<Unit, AccountError> {
        deletionRequestCount++
        return requestDeletionResult
    }

    override suspend fun signOut(): Result<Unit, AccountError> {
        signOutCount++
        return signOutResult
    }
}
