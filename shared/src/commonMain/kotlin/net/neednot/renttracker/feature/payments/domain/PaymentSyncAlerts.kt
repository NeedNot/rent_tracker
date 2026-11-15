package net.neednot.renttracker.feature.payments.domain

interface PaymentSyncAlerts {
    fun shouldExplain(): Boolean

    fun markExplained()

    fun requestPermission()
}
