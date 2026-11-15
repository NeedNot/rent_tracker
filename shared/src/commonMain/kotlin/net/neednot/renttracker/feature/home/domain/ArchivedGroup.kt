package net.neednot.renttracker.feature.home.domain

import kotlin.time.Instant

data class ArchivedGroup(
    val groupId: String,
    val name: String,
    val totalOwedCents: Long,
    val tenantCount: Int,
    val archivedAt: Instant,
)
