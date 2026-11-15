package net.neednot.renttracker.feature.home.domain

interface HomeLateRentAlerts {
    fun shouldAsk(): Boolean

    fun markAsked()

    fun requestPermission()
}
