package net.neednot.renttracker.feature.tenants.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.YearMonth
import kotlinx.serialization.builtins.serializer
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.data.AsOfClock
import net.neednot.renttracker.core.data.DismissedLateFeeSuggestionEntity
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.replaceTenant
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.sync.syncedFlow
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.tenants.domain.FirstRentTerm
import net.neednot.renttracker.feature.tenants.domain.RentTermDraft
import net.neednot.renttracker.feature.tenants.domain.RentTermPreview
import net.neednot.renttracker.feature.tenants.domain.TenantDetails
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
import net.neednot.renttracker.model.rentterms.RentTermsAddArgs
import net.neednot.renttracker.model.rentterms.RentTermsDeleteArgs
import net.neednot.renttracker.model.rentterms.RentTermsEditArgs
import net.neednot.renttracker.model.rentterms.RentTermsPreviewArgs
import net.neednot.renttracker.model.rentterms.RentTermsPreviewResult
import net.neednot.renttracker.model.tenants.TenantsAddArgs
import net.neednot.renttracker.model.tenants.TenantsArchiveArgs
import net.neednot.renttracker.model.tenants.TenantsEditArgs
import net.neednot.renttracker.model.tenants.TenantsRestoreArgs
import net.neednot.renttracker.model.tenants.TenantsScreenArgs
import net.neednot.renttracker.model.tenants.TenantsScreenResult

class TenantRepositoryImpl(
    backend: Backend,
    private val asOfClock: AsOfClock,
    private val database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
    telemetry: Telemetry,
) : TenantRepository {
    private val calls = TenantBackendCalls(backend, telemetry)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTenant(tenantId: String): Flow<Synced<TenantOverview?, TenantError>> =
        syncedFlow(
            cached = database.observeTenantOverview(tenantId, asOfClock.asOf),
            serverResults =
                asOfClock.asOf.flatMapLatest { asOf ->
                    calls.subscribe(
                        name = TENANT_SCREEN,
                        args = TenantsScreenArgs(asOf = asOf, tenantId = tenantId),
                        argsSerializer = TenantsScreenArgs.serializer(),
                        resultSerializer = TenantsScreenResult.serializer(),
                    )
                },
            store = { result -> screenResultStore.replaceTenant(tenantId, result) },
        )

    override suspend fun addTenant(
        groupId: String,
        details: TenantDetails,
        firstRentTerm: FirstRentTerm,
    ): Result<String, TenantError> =
        calls.mutate(
            name = ADD_TENANT,
            args = details.toAddArgs(groupId, firstRentTerm),
            argsSerializer = TenantsAddArgs.serializer(),
            resultSerializer = String.serializer(),
        )

    override suspend fun editTenant(
        tenantId: String,
        expectedVersion: Long,
        details: TenantDetails,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = EDIT_TENANT,
            args = details.toEditArgs(tenantId, expectedVersion),
            argsSerializer = TenantsEditArgs.serializer(),
        )

    override suspend fun archiveTenant(
        tenantId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = ARCHIVE_TENANT,
            args = TenantsArchiveArgs(expectedVersion = expectedVersion, tenantId = tenantId),
            argsSerializer = TenantsArchiveArgs.serializer(),
        )

    override suspend fun restoreTenant(
        tenantId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = RESTORE_TENANT,
            args = TenantsRestoreArgs(expectedVersion = expectedVersion, tenantId = tenantId),
            argsSerializer = TenantsRestoreArgs.serializer(),
        )

    override suspend fun addRentTerm(
        tenantId: String,
        draft: RentTermDraft,
    ): Result<String, TenantError> =
        calls.mutate(
            name = ADD_RENT_TERM,
            args = draft.toAddArgs(tenantId),
            argsSerializer = RentTermsAddArgs.serializer(),
            resultSerializer = String.serializer(),
        )

    override suspend fun editRentTerm(
        rentTermId: String,
        expectedVersion: Long,
        draft: RentTermDraft,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = EDIT_RENT_TERM,
            args = draft.toEditArgs(rentTermId, expectedVersion),
            argsSerializer = RentTermsEditArgs.serializer(),
        )

    override suspend fun deleteRentTerm(
        rentTermId: String,
        expectedVersion: Long,
        reason: String,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = DELETE_RENT_TERM,
            args = RentTermsDeleteArgs(expectedVersion = expectedVersion, reason = reason, rentTermId = rentTermId),
            argsSerializer = RentTermsDeleteArgs.serializer(),
        )

    override suspend fun previewRentTerm(
        tenantId: String,
        rentTermId: String?,
        draft: RentTermDraft,
    ): Result<RentTermPreview, TenantError> =
        calls
            .queryOnce(
                name = PREVIEW_RENT_TERM,
                args =
                    RentTermsPreviewArgs(
                        asOf = asOfClock.asOf.first(),
                        proposed = draft.toProposed(rentTermId),
                        tenantId = tenantId,
                    ),
                argsSerializer = RentTermsPreviewArgs.serializer(),
                resultSerializer = RentTermsPreviewResult.serializer(),
            ).map { it.toRentTermPreview() }

    override suspend fun dismissLateFeeSuggestion(
        tenantId: String,
        month: YearMonth,
    ) {
        database
            .dismissedLateFeeSuggestionDao()
            .insert(DismissedLateFeeSuggestionEntity(tenantId = tenantId, forMonth = month.toString()))
    }

    private companion object {
        const val TENANT_SCREEN = "tenants:screen"
        const val ADD_TENANT = "tenants:add"
        const val EDIT_TENANT = "tenants:edit"
        const val ARCHIVE_TENANT = "tenants:archive"
        const val RESTORE_TENANT = "tenants:restore"
        const val ADD_RENT_TERM = "rentTerms:add"
        const val EDIT_RENT_TERM = "rentTerms:edit"
        const val DELETE_RENT_TERM = "rentTerms:delete"
        const val PREVIEW_RENT_TERM = "rentTerms:preview"
    }
}
