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
    private val allocationDao = database.allocationDao()
    private val calendarMonthDao = database.calendarMonthDao()
    private val signedInAccountDao = database.signedInAccountDao()
    private val memberProfileDao = database.memberProfileDao()
    private val inviteDao = database.inviteDao()
    private val homeInvitationDao = database.homeInvitationDao()
    private val groupCacheDao = database.groupCacheDao()
    private val accountProfileDao = database.accountProfileDao()

    suspend fun replaceHome(
        groups: List<GroupEntity>,
        groupSummaries: List<GroupSummaryEntity>,
        invitations: List<HomeInvitationEntity>,
    ) = inOneTransaction {
        val groupIds = groups.map { it.id }
        groupDao.upsert(groups)
        groupDao.deleteAllExcept(groupIds)
        groupSummaryDao.upsert(groupSummaries)
        groupSummaryDao.deleteAllExcept(groupIds)
        homeInvitationDao.upsert(invitations)
        homeInvitationDao.deleteAllExcept(invitations.map { it.inviteId })
    }

    suspend fun replaceGroup(
        groupId: String,
        scope: GroupScopeEntities,
    ) = inOneTransaction {
        val tenantIds = scope.tenants.map { it.id }
        groupDao.upsert(scope.groups)
        groupSummaryDao.upsert(scope.groupSummaries)
        tenantSummaryDao.deleteInGroupExcept(groupId, tenantIds)
        tenantDao.deleteInGroupExcept(groupId, tenantIds)
        tenantDao.upsert(scope.tenants)
        tenantSummaryDao.upsert(scope.tenantSummaries)
        storeCallerMemberships(scope.callerMemberships)
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
        storeCallerMemberships(scope.callerMemberships)
        allocationDao.upsert(scope.allocations)
        allocationDao.deleteOfTenantExcept(tenantId, scope.allocations.map { it.key })
        scope.calendarMonths.map { it.month.substringBefore('-').toInt() }.distinct().forEach { year ->
            calendarMonthDao.deleteYear(tenantId, year)
        }
        calendarMonthDao.upsert(scope.calendarMonths)
    }

    suspend fun replaceCalendarYear(
        tenantId: String,
        year: Int,
        calendarMonths: List<CalendarMonthEntity>,
    ) = inOneTransaction {
        calendarMonthDao.deleteYear(tenantId, year)
        calendarMonthDao.upsert(calendarMonths)
    }

    suspend fun replaceMembers(
        groupId: String,
        memberships: List<MembershipEntity>,
        memberProfiles: List<MemberProfileEntity>,
        invites: List<InviteEntity>,
    ) = inOneTransaction {
        membershipDao.deleteInGroupExcept(groupId, memberships.map { it.id })
        membershipDao.upsert(memberships)
        memberProfileDao.upsert(memberProfiles)
        inviteDao.deleteInGroupExcept(groupId, invites.map { it.id })
        inviteDao.upsert(invites)
    }

    suspend fun replaceAccountProfile(profile: AccountProfileEntity) =
        inOneTransaction {
            accountProfileDao.deleteAll()
            accountProfileDao.upsert(profile)
        }

    suspend fun clearGroup(groupId: String) =
        inOneTransaction {
            with(groupCacheDao) {
                deleteRentTerms(groupId)
                deletePayments(groupId)
                deleteDebts(groupId)
                deleteLedgerRows(groupId)
                deleteAllocations(groupId)
                deleteCalendarMonths(groupId)
                deleteTenantSummaries(groupId)
                deleteTenants(groupId)
                deleteMemberships(groupId)
                deleteInvites(groupId)
                deleteGroupSummary(groupId)
                deleteGroup(groupId)
            }
        }

    private suspend fun storeCallerMemberships(callerMemberships: List<MembershipEntity>) {
        callerMemberships.forEach { caller ->
            signedInAccountDao.deleteAllExcept(caller.accountId)
            signedInAccountDao.upsert(SignedInAccountEntity(caller.accountId))
            membershipDao.deleteOtherMembershipsOfAccount(caller.groupId, caller.accountId, caller.id)
        }
        membershipDao.upsert(callerMemberships)
    }

    private suspend fun inOneTransaction(write: suspend () -> Unit) {
        database.useWriterConnection { connection -> connection.immediateTransaction { write() } }
    }
}
