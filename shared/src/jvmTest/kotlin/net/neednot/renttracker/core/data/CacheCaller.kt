package net.neednot.renttracker.core.data

suspend fun RentTrackerDatabase.cacheCaller(membership: MembershipEntity) {
    signedInAccountDao().upsert(SignedInAccountEntity(membership.accountId))
    membershipDao().upsert(listOf(membership))
}
