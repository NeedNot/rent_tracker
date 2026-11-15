package net.neednot.renttracker.feature.home.presentation.components

data class RejectedPaymentActions(
    val onCardClicked: () -> Unit,
    val onPaymentDismissed: (clientId: String) -> Unit,
    val onSheetDismissed: () -> Unit,
) {
    companion object {
        val None = RejectedPaymentActions({}, {}, {})
    }
}
