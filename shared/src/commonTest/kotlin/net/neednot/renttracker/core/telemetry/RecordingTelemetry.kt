package net.neednot.renttracker.core.telemetry

class RecordingTelemetry : Telemetry {
    val logs = mutableListOf<String>()

    override fun log(
        tag: String,
        message: String,
        severity: Severity,
    ) {
        logs += message
    }

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
