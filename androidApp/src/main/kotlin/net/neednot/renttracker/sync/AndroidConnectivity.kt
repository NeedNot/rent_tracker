package net.neednot.renttracker.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.neednot.renttracker.core.data.Connectivity

class AndroidConnectivity(
    context: Context,
) : Connectivity {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val online = MutableStateFlow(connectivityManager.activeNetwork.hasInternet())

    override val isOnline: StateFlow<Boolean> = online.asStateFlow()

    init {
        connectivityManager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities,
                ) {
                    online.value = capabilities.hasInternet()
                }

                override fun onLost(network: Network) {
                    online.value = false
                }
            },
        )
    }

    private fun Network?.hasInternet(): Boolean =
        this != null && connectivityManager.getNetworkCapabilities(this)?.hasInternet() == true

    private fun NetworkCapabilities.hasInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
