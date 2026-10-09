package com.sk.wifi_internetspeedtester.domain.model

import java.util.UUID

enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    DISCONNECTED
}

data class NetworkStatus(
    val type: NetworkType = NetworkType.DISCONNECTED,
    val isConnected: Boolean = false,
    val isInternetAvailable: Boolean = false,
    val ssid: String? = null,
    val rssi: Int = 0,
    val signalPercentage: Int = 0,
    val linkSpeedMbps: Int = 0,
    val ipAddress: String? = null
)

enum class SpeedTestStage {
    IDLE,
    CONNECTING,
    PING,
    DOWNLOAD,
    UPLOAD,
    FINALIZING,
    COMPLETED,
    ERROR,
    CANCELLED
}

data class SpeedTestServer(
    val id: String,
    val name: String,
    val location: String,
    val host: String,
    val pingUrl: String,
    val downloadUrl: String,
    val uploadUrl: String
)

data class SpeedTestState(
    val stage: SpeedTestStage = SpeedTestStage.IDLE,
    val progress: Float = 0f,
    val currentSpeedMbps: Double = 0.0,
    val pingMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val packetLossPercent: Double = 0.0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val serverName: String = "Auto Selected",
    val serverLocation: String = "Optimal Server",
    val errorMessage: String? = null
)

enum class RatingLevel {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

data class SpeedTestResult(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val packetLossPercent: Double = 0.0,
    val stabilityPercent: Double,
    val durationSeconds: Double,
    val networkType: NetworkType,
    val networkName: String,
    val serverName: String,
    val networkTypeLabel: String = "Wi-Fi",
    val ispName: String? = null,
    val locationName: String? = null,
    val overallQuality: RatingLevel = RatingLevel.GOOD
)

data class ActivityCapability(
    val name: String,
    val rating: RatingLevel,
    val description: String,
    val iconType: String
)

data class SmartExplanation(
    val headline: String,
    val summary: String,
    val capabilities: List<ActivityCapability>,
    val troubleshootingTips: List<String>
)
