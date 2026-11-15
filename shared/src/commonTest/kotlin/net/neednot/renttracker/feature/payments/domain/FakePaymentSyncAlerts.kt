package net.neednot.renttracker.feature.payments.domain

class FakePaymentSyncAlerts(
    var isExplanationDue: Boolean = false,
) : PaymentSyncAlerts {
    var explainedCount = 0
    var permissionRequestCount = 0

    override fun shouldExplain(): Boolean = isExplanationDue

    override fun markExplained() {
        explainedCount++
    }

    override fun requestPermission() {
        permissionRequestCount++
    }
}
