package com.sk.wifi_internetspeedtester.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.sk.wifi_internetspeedtester.domain.model.NetworkStatus
import com.sk.wifi_internetspeedtester.domain.model.NetworkType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

class NetworkRepository(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    fun getNetworkStatusFlow(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentNetworkStatus())
            }

            override fun onLost(network: Network) {
                trySend(getCurrentNetworkStatus())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getCurrentNetworkStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        // Initial state emission
        trySend(getCurrentNetworkStatus())

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    fun getCurrentNetworkStatus(): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork
            ?: return NetworkStatus(type = NetworkType.DISCONNECTED, isConnected = false)

        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkStatus(type = NetworkType.DISCONNECTED, isConnected = false)

        val isInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiInfo = wifiManager?.connectionInfo
                val rawSsid = wifiInfo?.ssid?.replace("\"", "")
                val ssid = if (rawSsid != null && rawSsid != "<unknown ssid>") rawSsid else "Wi-Fi Network"
                val rssi = wifiInfo?.rssi ?: -100
                val signalPercentage = calculateSignalPercentage(rssi)
                val linkSpeed = wifiInfo?.linkSpeed ?: 0
                val ip = formatIpAddress(wifiInfo?.ipAddress ?: 0)

                NetworkStatus(
                    type = NetworkType.WIFI,
                    isConnected = true,
                    isInternetAvailable = isInternet,
                    ssid = ssid,
                    rssi = rssi,
                    signalPercentage = signalPercentage,
                    linkSpeedMbps = if (linkSpeed > 0) linkSpeed else 0,
                    ipAddress = ip
                )
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                NetworkStatus(
                    type = NetworkType.CELLULAR,
                    isConnected = true,
                    isInternetAvailable = isInternet,
                    ssid = "Mobile Data",
                    signalPercentage = 85
                )
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                NetworkStatus(
                    type = NetworkType.ETHERNET,
                    isConnected = true,
                    isInternetAvailable = isInternet,
                    ssid = "Ethernet Connection",
                    signalPercentage = 100
                )
            }
            else -> {
                NetworkStatus(
                    type = NetworkType.DISCONNECTED,
                    isConnected = false
                )
            }
        }
    }

    private fun calculateSignalPercentage(rssi: Int): Int {
        if (rssi <= -100) return 0
        if (rssi >= -50) return 100
        return WifiManager.calculateSignalLevel(rssi, 100)
    }

    private fun formatIpAddress(ip: Int): String? {
        if (ip == 0) return null
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }
}
