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
    private val paymentDao = database.paymentDao()
    private val debtDao = database.debtDao()
    private val accountNameDao = database.accountNameDao()
    private val membershipDao = database.membershipDao()
    private val ledgerRowDao = database.ledgerRowDao()

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
        tenantSummaries: List<TenantSummaryEntity>,
        scope: TenantScopeEntities,
    ) = inOneTransaction {
        groupDao.upsert(groups)
        tenantDao.upsert(tenants)
        tenantSummaryDao.upsert(tenantSummaries)
        rentTermDao.upsert(scope.rentTerms)
        rentTermDao.deleteOfTenantExcept(tenantId, scope.rentTerms.map { it.id })
        paymentDao.upsert(scope.payments)
        paymentDao.deleteOfTenantExcept(tenantId, scope.payments.map { it.id })
        debtDao.upsert(scope.debts)
        debtDao.deleteOfTenantExcept(tenantId, scope.debts.map { it.id })
        ledgerRowDao.upsert(scope.ledgerRows)
        ledgerRowDao.deleteOfTenantExcept(tenantId, scope.ledgerRows.map { it.key })
        accountNameDao.upsert(scope.accountNames)
        membershipDao.upsert(scope.memberships)
        membershipDao.deleteInGroupsExcept(groups.map { it.id }, scope.memberships.map { it.id })
    }

    private suspend fun inOneTransaction(write: suspend () -> Unit) {
        database.useWriterConnection { connection -> connection.immediateTransaction { write() } }
    }
}
