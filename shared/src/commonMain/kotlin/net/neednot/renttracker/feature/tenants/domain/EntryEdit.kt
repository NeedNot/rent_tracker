package net.neednot.renttracker.feature.tenants.domain

import kotlin.time.Instant

data class EntryEdit(
    val by: Person,
    val at: Instant,
)
