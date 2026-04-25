package com.app.secondserving.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitor de conectividad basado en ConnectivityManager.registerNetworkCallback.
 *
 * Es la API recomendada por el sistema desde Android N en adelante — los
 * BroadcastReceiver con CONNECTIVITY_ACTION declarados en el manifest son
 * ignorados por el sistema (curso "Conectivity 2.0.pdf"). Aquí se registra
 * el callback en runtime, dentro del ciclo de vida del proceso.
 *
 * Expone un StateFlow<Boolean> que las pantallas observan para mostrar /
 * ocultar el banner de offline. Singleton: se instancia una sola vez en
 * SecondServingApp.onCreate().
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(computeOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = computeOnline()
        }

        override fun onLost(network: Network) {
            _isOnline.value = computeOnline()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            // Detecta también el caso en el que hay red pero sin internet
            // validado (ej. wifi del aeropuerto sin pasar el captive portal).
            _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun computeOnline(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
