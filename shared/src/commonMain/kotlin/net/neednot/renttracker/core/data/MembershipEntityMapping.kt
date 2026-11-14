package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.tenants.Account
import net.neednot.renttracker.model.tenants.Membership
import net.neednot.renttracker.model.tenants.MembershipDebts
import net.neednot.renttracker.model.tenants.MembershipMembers
import net.neednot.renttracker.model.tenants.MembershipPayments
import net.neednot.renttracker.model.tenants.MembershipRentTerms
import net.neednot.renttracker.model.tenants.MembershipSettings
import net.neednot.renttracker.model.tenants.MembershipTenants
import net.neednot.renttracker.model.tenants.MembershipRole as ConvexMembershipRole

fun Account.toEntity(): AccountNameEntity = AccountNameEntity(accountId = accountId, displayName = displayName)

fun Membership.toEntity(): MembershipEntity =
    MembershipEntity(
        id = id,
        groupId = groupId,
        accountId = accountId,
        role = role.toEntity(),
        tenants = tenants.toEntity(),
        rentTerms = rentTerms.toEntity(),
        payments = payments.toEntity(),
        debts = debts.toEntity(),
        members = members.toEntity(),
        settings = settings.toEntity(),
        editOthers = editOthers,
    )

private fun ConvexMembershipRole.toEntity(): MembershipRole =
    when (this) {
        ConvexMembershipRole.Owner -> MembershipRole.Owner
        ConvexMembershipRole.Member -> MembershipRole.Member
    }

private fun MembershipTenants.toEntity(): AccessLevel =
    when (this) {
        MembershipTenants.Read -> AccessLevel.Read
        MembershipTenants.AddEdit -> AccessLevel.AddEdit
        MembershipTenants.Full -> AccessLevel.Full
    }

private fun MembershipRentTerms.toEntity(): AccessLevel =
    when (this) {
        MembershipRentTerms.Read -> AccessLevel.Read
        MembershipRentTerms.AddEdit -> AccessLevel.AddEdit
        MembershipRentTerms.Full -> AccessLevel.Full
    }

private fun MembershipPayments.toEntity(): AccessLevel =
    when (this) {
        MembershipPayments.Read -> AccessLevel.Read
        MembershipPayments.AddEdit -> AccessLevel.AddEdit
        MembershipPayments.Full -> AccessLevel.Full
    }

private fun MembershipDebts.toEntity(): AccessLevel =
    when (this) {
        MembershipDebts.Read -> AccessLevel.Read
        MembershipDebts.AddEdit -> AccessLevel.AddEdit
        MembershipDebts.Full -> AccessLevel.Full
    }

private fun MembershipMembers.toEntity(): AccessLevel =
    when (this) {
        MembershipMembers.Read -> AccessLevel.Read
        MembershipMembers.AddEdit -> AccessLevel.AddEdit
        MembershipMembers.Full -> AccessLevel.Full
    }

private fun MembershipSettings.toEntity(): SettingsAccessLevel =
    when (this) {
        MembershipSettings.Read -> SettingsAccessLevel.Read
        MembershipSettings.Full -> SettingsAccessLevel.Full
    }
