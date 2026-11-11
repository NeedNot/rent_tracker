package net.neednot.renttracker.core.telemetry

interface Telemetry {
    fun log(
        tag: String,
        message: String,
        severity: Severity = Severity.Info,
    )

    fun recordException(
        throwable: Throwable,
        severity: Severity = Severity.Error,
        attributes: Map<String, String> = emptyMap(),
    )

    fun recordEvent(
        name: String,
        properties: Map<String, String> = emptyMap(),
    )
}
