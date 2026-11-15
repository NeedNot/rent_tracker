package net.neednot.renttracker.core.data

interface SyncAlerts {
    fun shouldExplain(): Boolean

    fun markExplained()

    fun requestPermission()
}
