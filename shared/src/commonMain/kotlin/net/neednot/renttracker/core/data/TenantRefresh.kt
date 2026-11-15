package net.neednot.renttracker.core.data

sealed interface TenantRefresh {
    data class Current(
        val fingerprint: String,
        val tenant: TenantEntity?,
    ) : TenantRefresh {
        fun isActiveIn(groupId: String): Boolean =
            tenant != null && tenant.groupId == groupId && tenant.status == TenantStatus.Active
    }

    data object NotVisible : TenantRefresh

    data object Unreachable : TenantRefresh
}
