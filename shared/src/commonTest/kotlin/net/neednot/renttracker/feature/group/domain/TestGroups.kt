package net.neednot.renttracker.feature.group.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.data.PermissionSet
import kotlin.time.Instant

fun groupDetails(
    activeTenants: List<GroupTenant> = emptyList(),
    archivedTenants: List<GroupTenant> = emptyList(),
    totalOwedCents: Long = 0,
    lateTenantCount: Int = 0,
    archivedOwedCents: Long = 0,
    access: CallerAccess = ownerAccess(),
    settings: GroupSettings = groupSettings(),
    version: Long = 3,
    archivedAt: Instant? = null,
) = GroupDetails(
    groupId = "g1",
    name = settings.name,
    settings = settings,
    version = version,
    archivedAt = archivedAt,
    totalOwedCents = totalOwedCents,
    lateTenantCount = lateTenantCount,
    archivedOwedCents = archivedOwedCents,
    activeTenants = activeTenants,
    archivedTenants = archivedTenants,
    access = access,
)

fun groupSettings(
    name: String = "1st Street",
    timeZone: String = "America/Los_Angeles",
    paymentOrder: PaymentOrder = PaymentOrder.RentFirst,
    graceDays: Int = 5,
    lateFeeFlatCents: Long? = 5_000,
) = GroupSettings(name, timeZone, paymentOrder, graceDays, lateFeeFlatCents)

fun groupTenant(
    tenantId: String,
    name: String = "Tenant $tenantId",
    contactName: String? = null,
    nextDueDate: LocalDate? = null,
    state: GroupTenantState = GroupTenantState.PaidUp,
    balance: TenantBalance = TenantBalance.PaidUp,
) = GroupTenant(
    tenantId = tenantId,
    name = name,
    contactName = contactName,
    nextDueDate = nextDueDate,
    state = state,
    balance = balance,
)

fun ownerAccess(groupArchived: Boolean = false) =
    CallerAccess(
        membershipId = "m1",
        accountId = "a1",
        role = MembershipRole.Owner,
        permissions = PermissionPreset.ownerPermissions,
        groupArchived = groupArchived,
    )

fun memberAccess(
    permissions: PermissionSet = PermissionPreset.Manager.permissions,
    groupArchived: Boolean = false,
) = CallerAccess(
    membershipId = "m2",
    accountId = "a2",
    role = MembershipRole.Member,
    permissions = permissions,
    groupArchived = groupArchived,
)
