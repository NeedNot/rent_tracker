package net.neednot.renttracker.core.data

data class PermissionSet(
    val tenants: AccessLevel,
    val rentTerms: AccessLevel,
    val payments: AccessLevel,
    val debts: AccessLevel,
    val members: AccessLevel,
    val settings: SettingsAccessLevel,
    val editOthers: Boolean,
)
