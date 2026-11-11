package net.neednot.renttracker.core.telemetry

class NoOpTelemetry : Telemetry {
    override fun log(
        tag: String,
        message: String,
        severity: Severity,
    ) = Unit

    override fun recordException(
        throwable: Throwable,
        severity: Severity,
        attributes: Map<String, String>,
    ) = Unit

    override fun recordEvent(
        name: String,
        properties: Map<String, String>,
    ) = Unit
}
