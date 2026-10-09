package com.sk.wifi_internetspeedtester.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.sk.wifi_internetspeedtester.domain.model.NetworkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object NetworkDetailsProvider {

    fun getNetworkTypeLabel(context: Context, type: NetworkType): String {
        return when (type) {
            NetworkType.WIFI -> "Wi-Fi"
            NetworkType.ETHERNET -> "Ethernet"
            NetworkType.DISCONNECTED -> "Disconnected"
            NetworkType.CELLULAR -> getCellularGenerationLabel(context)
        }
    }

    private fun getCellularGenerationLabel(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return "Mobile Data"

        val hasPhonePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPhonePermission) {
            return "Mobile Data"
        }

        val networkType = try {
            @Suppress("MissingPermission", "DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                telephonyManager.dataNetworkType
            } else {
                telephonyManager.networkType
            }
        } catch (_: Exception) {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }

        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
            else -> "Mobile Data"
        }
    }

    fun getIspName(context: Context): String? {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val simName = telephonyManager?.simOperatorName?.takeIf { it.isNotBlank() }
        val netName = telephonyManager?.networkOperatorName?.takeIf { it.isNotBlank() }

        val carrierName = simName ?: netName
        if (carrierName != null && carrierName != "Android") {
            return carrierName
        }

        return null
    }

    suspend fun getLocationName(context: Context): String? = withContext(Dispatchers.IO) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return@withContext null
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext null

            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val loc = try { locationManager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
                if (loc != null) {
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            }

            if (bestLocation == null) return@withContext null

            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea
                val country = address.countryName
                return@withContext when {
                    city != null && country != null -> "$city, $country"
                    city != null -> city
                    country != null -> country
                    else -> null
                }
            }
        } catch (_: Exception) {
            return@withContext null
        }

        return@withContext null
    }
}
