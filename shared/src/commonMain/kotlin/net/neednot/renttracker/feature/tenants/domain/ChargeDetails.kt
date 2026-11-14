package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class ChargeDetails(
    val debtId: String,
    val version: Long,
    val kind: ChargeKind,
    val note: String?,
    val date: LocalDate,
    val addedAt: Instant,
    val addedBy: Person,
    val status: EntryStatus,
    val edit: EntryEdit?,
    val mayNotApply: Boolean,
    val canChange: Boolean,
)
