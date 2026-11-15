package net.neednot.renttracker.feature.home.domain

class FakeHomeLateRentAlerts(
    var wouldAsk: Boolean = false,
) : HomeLateRentAlerts {
    var askedCount = 0
        private set
    var permissionRequests = 0
        private set

    override fun shouldAsk(): Boolean = wouldAsk && askedCount == 0

    override fun markAsked() {
        askedCount++
    }

    override fun requestPermission() {
        permissionRequests++
    }
}
