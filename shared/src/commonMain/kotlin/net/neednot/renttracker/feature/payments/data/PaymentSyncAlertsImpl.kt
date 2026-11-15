package net.neednot.renttracker.feature.payments.data

import net.neednot.renttracker.core.data.SyncAlerts
import net.neednot.renttracker.feature.payments.domain.PaymentSyncAlerts

class PaymentSyncAlertsImpl(
    private val syncAlerts: SyncAlerts,
) : PaymentSyncAlerts {
    override fun shouldExplain(): Boolean = syncAlerts.shouldExplain()

    override fun markExplained() {
        syncAlerts.markExplained()
    }

    override fun requestPermission() {
        syncAlerts.requestPermission()
    }
}
