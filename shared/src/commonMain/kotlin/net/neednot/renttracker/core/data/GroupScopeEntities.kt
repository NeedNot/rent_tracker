package net.neednot.renttracker.core.data

data class GroupScopeEntities(
    val groups: List<GroupEntity>,
    val tenants: List<TenantEntity>,
    val tenantSummaries: List<TenantSummaryEntity>,
    val groupSummaries: List<GroupSummaryEntity>,
    val callerMemberships: List<MembershipEntity>,
)
