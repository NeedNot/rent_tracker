package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.feature.tenants.domain.PendingPayment

internal data class OfflineSources(
    val pendingPayments: List<PendingPayment>,
    val freshness: Freshness,
)
