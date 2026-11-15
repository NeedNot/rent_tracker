package net.neednot.renttracker.push

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import net.neednot.renttracker.auth.ForegroundActivity
import net.neednot.renttracker.core.data.LateRentAlerts

class AndroidLateRentAlerts(
    private val context: Context,
    private val foregroundActivity: ForegroundActivity,
) : LateRentAlerts {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override fun shouldAsk(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission() &&
            !preferences.getBoolean(ASKED, false)

    override fun markAsked() {
        preferences.edit().putBoolean(ASKED, true).apply()
    }

    override fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        foregroundActivity.current?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
    }

    override fun areBlockedOnPhone(): Boolean = !notificationManager.areNotificationsEnabled()

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val PREFERENCES = "late_rent_alerts"
        const val ASKED = "asked"
        const val REQUEST_CODE = 42
    }
}
