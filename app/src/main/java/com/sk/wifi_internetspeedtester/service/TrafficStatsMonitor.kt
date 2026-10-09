package com.sk.wifi_internetspeedtester.service

import android.net.TrafficStats
import java.util.Locale

data class LiveTrafficData(
    val downloadBytesPerSec: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val formattedDownloadSpeed: String = "0 Kbps",
    val formattedUploadSpeed: String = "0 Kbps",
    val formattedDownloadBytesRate: String = "0 B/s",
    val formattedUploadBytesRate: String = "0 B/s"
)

object TrafficStatsMonitor {

    private var previousRxBytes = -1L
    private var previousTxBytes = -1L
    private var previousTimeMs = -1L

    fun sampleLiveTraffic(): LiveTrafficData {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()

        if (previousRxBytes == -1L || previousTxBytes == -1L || previousTimeMs == -1L) {
            previousRxBytes = currentRx
            previousTxBytes = currentTx
            previousTimeMs = currentTime
            return LiveTrafficData()
        }

        val timeDiffSec = (currentTime - previousTimeMs) / 1000.0
        if (timeDiffSec <= 0.1) {
            return LiveTrafficData()
        }

        val rxDiff = if (currentRx >= previousRxBytes) currentRx - previousRxBytes else 0L
        val txDiff = if (currentTx >= previousTxBytes) currentTx - previousTxBytes else 0L

        previousRxBytes = currentRx
        previousTxBytes = currentTx
        previousTimeMs = currentTime

        val downloadRateBytes = (rxDiff / timeDiffSec).toLong()
        val uploadRateBytes = (txDiff / timeDiffSec).toLong()

        val downloadMbpsVal = (downloadRateBytes * 8.0) / 1_000_000.0
        val uploadMbpsVal = (uploadRateBytes * 8.0) / 1_000_000.0

        return LiveTrafficData(
            downloadBytesPerSec = downloadRateBytes,
            uploadBytesPerSec = uploadRateBytes,
            downloadMbps = downloadMbpsVal,
            uploadMbps = uploadMbpsVal,
            formattedDownloadSpeed = formatMbps(downloadRateBytes),
            formattedUploadSpeed = formatMbps(uploadRateBytes),
            formattedDownloadBytesRate = formatByteRate(downloadRateBytes),
            formattedUploadBytesRate = formatByteRate(uploadRateBytes)
        )
    }

    fun formatMbps(bytesPerSec: Long): String {
        val mbps = (bytesPerSec * 8.0) / 1_000_000.0
        return when {
            mbps >= 10.0 -> String.format(Locale.US, "%.1f Mbps", mbps)
            mbps >= 0.1 -> String.format(Locale.US, "%.2f Mbps", mbps)
            else -> {
                val kbps = (bytesPerSec * 8.0) / 1000.0
                String.format(Locale.US, "%.0f Kbps", kbps)
            }
        }
    }

    fun formatByteRate(bytesPerSec: Long): String {
        if (bytesPerSec < 1024) {
            return "$bytesPerSec B/s"
        }
        val kbps = bytesPerSec / 1024.0
        if (kbps < 1024) {
            return String.format(Locale.US, "%.1f KB/s", kbps)
        }
        val mbps = kbps / 1024.0
        return String.format(Locale.US, "%.2f MB/s", mbps)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024 * 1024) {
            val kb = bytes / 1024.0
            return String.format(Locale.US, "%.1f KB", kb)
        }
        val mb = bytes / (1024.0 * 1024.0)
        if (mb < 1024) {
            return String.format(Locale.US, "%.1f MB", mb)
        }
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }
}
