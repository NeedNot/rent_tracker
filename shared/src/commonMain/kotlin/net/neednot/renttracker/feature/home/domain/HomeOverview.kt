package net.neednot.renttracker.feature.home.domain

data class HomeOverview(
    val groups: List<GroupOverview>,
    val invitations: List<HomeInvitation>,
    val archivedGroupCount: Int,
) {
    val isEmpty: Boolean get() = groups.isEmpty() && invitations.isEmpty() && archivedGroupCount == 0
}
