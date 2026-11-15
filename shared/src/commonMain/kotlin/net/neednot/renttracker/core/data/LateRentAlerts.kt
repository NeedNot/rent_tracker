package net.neednot.renttracker.core.data

interface LateRentAlerts {
    fun shouldAsk(): Boolean

    fun markAsked()

    fun requestPermission()

    fun areBlockedOnPhone(): Boolean
}
