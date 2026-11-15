package net.neednot.renttracker.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import net.neednot.renttracker.auth.ForegroundActivity
import net.neednot.renttracker.core.data.SyncAlerts

class AndroidSyncAlerts(
    private val context: Context,
    private val foregroundActivity: ForegroundActivity,
) : SyncAlerts {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun shouldExplain(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission() &&
            !preferences.getBoolean(EXPLAINED, false)

    override fun markExplained() {
        preferences.edit().putBoolean(EXPLAINED, true).apply()
    }

    override fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        foregroundActivity.current?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val PREFERENCES = "sync_alerts"
        const val EXPLAINED = "explained"
        const val REQUEST_CODE = 41
    }
}
