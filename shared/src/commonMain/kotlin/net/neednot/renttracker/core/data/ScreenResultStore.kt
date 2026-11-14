package net.neednot.renttracker.core.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

class ScreenResultStore(
    private val database: RentTrackerDatabase,
) {
    private val groupDao = database.groupDao()
    private val groupSummaryDao = database.groupSummaryDao()
    private val tenantDao = database.tenantDao()
    private val tenantSummaryDao = database.tenantSummaryDao()
    private val rentTermDao = database.rentTermDao()

    suspend fun replaceHome(
        groups: List<GroupEntity>,
        groupSummaries: List<GroupSummaryEntity>,
    ) = inOneTransaction {
        val groupIds = groups.map { it.id }
        groupDao.upsert(groups)
        groupDao.deleteAllExcept(groupIds)
        groupSummaryDao.upsert(groupSummaries)
        groupSummaryDao.deleteAllExcept(groupIds)
    }

    suspend fun replaceGroup(
        groupId: String,
        groups: List<GroupEntity>,
        tenants: List<TenantEntity>,
        tenantSummaries: List<TenantSummaryEntity>,
        groupSummaries: List<GroupSummaryEntity>,
    ) = inOneTransaction {
        val tenantIds = tenants.map { it.id }
        groupDao.upsert(groups)
        groupSummaryDao.upsert(groupSummaries)
        tenantSummaryDao.deleteInGroupExcept(groupId, tenantIds)
        tenantDao.deleteInGroupExcept(groupId, tenantIds)
        tenantDao.upsert(tenants)
        tenantSummaryDao.upsert(tenantSummaries)
    }

    suspend fun replaceTenant(
        tenantId: String,
        groups: List<GroupEntity>,
        tenants: List<TenantEntity>,
        rentTerms: List<RentTermEntity>,
        tenantSummaries: List<TenantSummaryEntity>,
    ) = inOneTransaction {
        groupDao.upsert(groups)
        tenantDao.upsert(tenants)
        tenantSummaryDao.upsert(tenantSummaries)
        rentTermDao.upsert(rentTerms)
        rentTermDao.deleteOfTenantExcept(tenantId, rentTerms.map { it.id })
    }

    private suspend fun inOneTransaction(write: suspend () -> Unit) {
        database.useWriterConnection { connection -> connection.immediateTransaction { write() } }
    }
}
