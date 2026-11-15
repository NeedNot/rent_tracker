package net.neednot.renttracker.core.data

import com.github.michaelbull.result.fold
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.model.tenants.TenantsScreenArgs
import net.neednot.renttracker.model.tenants.TenantsScreenResult
import kotlin.time.Duration.Companion.seconds

class TenantRefresher(
    private val backend: Backend,
    database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
    private val asOfClock: AsOfClock,
) {
    private val dao = database.pendingPaymentDao()

    suspend fun refresh(tenantId: String): TenantRefresh {
        val args = TenantsScreenArgs(asOf = asOfClock.asOf.first(), tenantId = tenantId)
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend
                    .subscribe(
                        TENANT_SCREEN,
                        ConvexJson.encodeArgs(TenantsScreenArgs.serializer(), args),
                        TenantsScreenResult.serializer(),
                    ).first()
            } ?: return TenantRefresh.Unreachable
        return answer.fold(
            success = { result ->
                screenResultStore.replaceTenant(tenantId, result)
                TenantRefresh.Current(dao.tenantFingerprint(tenantId), dao.tenant(tenantId))
            },
            failure = { error -> if (error.isRetryable()) TenantRefresh.Unreachable else TenantRefresh.NotVisible },
        )
    }

    private companion object {
        const val TENANT_SCREEN = "tenants:screen"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
