package net.neednot.renttracker.core.data

class FakeLateRentAlerts(
    var blockedOnPhone: Boolean = false,
) : LateRentAlerts {
    override fun shouldAsk(): Boolean = false

    override fun markAsked() = Unit

    override fun requestPermission() = Unit

    override fun areBlockedOnPhone(): Boolean = blockedOnPhone
}
