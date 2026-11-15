package net.neednot.renttracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.neednot.renttracker.app.App
import net.neednot.renttracker.core.data.PendingPaymentLink
import net.neednot.renttracker.core.data.PendingPaymentLinks
import net.neednot.renttracker.sync.SyncProblemNotifier
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val pendingPaymentLinks: PendingPaymentLinks by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) openPendingPayment(intent)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openPendingPayment(intent)
    }

    private fun openPendingPayment(intent: Intent) {
        val tenantId = intent.getStringExtra(SyncProblemNotifier.EXTRA_TENANT_ID) ?: return
        val clientId = intent.getStringExtra(SyncProblemNotifier.EXTRA_CLIENT_ID) ?: return
        pendingPaymentLinks.open(PendingPaymentLink(tenantId, clientId))
    }
}
