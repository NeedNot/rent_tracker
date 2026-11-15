package net.neednot.renttracker.feature.home.data

import net.neednot.renttracker.core.data.LateRentAlerts
import net.neednot.renttracker.feature.home.domain.HomeLateRentAlerts

class HomeLateRentAlertsImpl(
    private val lateRentAlerts: LateRentAlerts,
) : HomeLateRentAlerts {
    override fun shouldAsk(): Boolean = lateRentAlerts.shouldAsk()

    override fun markAsked() {
        lateRentAlerts.markAsked()
    }

    override fun requestPermission() {
        lateRentAlerts.requestPermission()
    }
}
