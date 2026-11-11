package net.neednot.renttracker.core.telemetry

import android.util.Log

class LogcatTelemetry : Telemetry {
    override fun log(
        tag: String,
        message: String,
        severity: Severity,
    ) {
        when (severity) {
            Severity.Debug -> Log.d(tag, message)
            Severity.Info -> Log.i(tag, message)
            Severity.Warning -> Log.w(tag, message)
            Severity.Error -> Log.e(tag, message)
            Severity.Critical -> Log.wtf(tag, message)
        }
    }

    override fun recordException(
        throwable: Throwable,
        severity: Severity,
        attributes: Map<String, String>,
    ) {
        val message =
            listOfNotNull(
                throwable.message ?: throwable.toString(),
                attributes.ifEmpty { null },
            ).joinToString(" ")
        when (severity) {
            Severity.Critical -> Log.wtf(TAG, message, throwable)
            else -> Log.e(TAG, message, throwable)
        }
    }

    override fun recordEvent(
        name: String,
        properties: Map<String, String>,
    ) {
        Log.i(TAG, listOfNotNull("EVENT $name", properties.ifEmpty { null }).joinToString(" "))
    }

    private companion object {
        const val TAG = "Telemetry"
    }
}
