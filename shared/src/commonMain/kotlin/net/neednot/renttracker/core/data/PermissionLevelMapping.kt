package net.neednot.renttracker.core.data

// Codegen emits one level enum per field (MembershipTenants, InvitePermissionsDebts, ...), all with the entry
// names of AccessLevel and SettingsAccessLevel, so levels convert by name instead of one `when` per generated enum.
fun accessLevelOf(level: Enum<*>): AccessLevel = AccessLevel.valueOf(level.name)

fun settingsAccessLevelOf(level: Enum<*>): SettingsAccessLevel = SettingsAccessLevel.valueOf(level.name)

inline fun <reified E : Enum<E>> AccessLevel.toConvex(): E = enumValueOf(name)

inline fun <reified E : Enum<E>> SettingsAccessLevel.toConvex(): E = enumValueOf(name)
