package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.access.PermissionCategory

enum class PermissionArea(
    val category: PermissionCategory?,
) {
    Tenants(PermissionCategory.Tenants),
    RentTerms(PermissionCategory.RentTerms),
    Payments(PermissionCategory.Payments),
    Debts(PermissionCategory.Debts),
    Members(PermissionCategory.Members),
    Settings(null),
}
