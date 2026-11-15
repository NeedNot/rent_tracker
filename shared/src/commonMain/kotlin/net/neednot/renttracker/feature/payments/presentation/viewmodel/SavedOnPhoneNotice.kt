package net.neednot.renttracker.feature.payments.presentation.viewmodel

import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.domain.PaymentSyncAlerts
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_saved_on_phone

internal class SavedOnPhoneNotice(
    private val syncAlerts: PaymentSyncAlerts,
    private val messageDispatcher: MessageDispatcher,
) {
    fun needsExplanation(): Boolean = syncAlerts.shouldExplain()

    fun explained(answer: SyncAlertsAnswer) {
        syncAlerts.markExplained()
        if (answer == SyncAlertsAnswer.Allow) syncAlerts.requestPermission()
        announce()
    }

    fun announce() {
        messageDispatcher.show(UiMessage(UiText.Resource(Res.string.payment_saved_on_phone)))
    }
}
