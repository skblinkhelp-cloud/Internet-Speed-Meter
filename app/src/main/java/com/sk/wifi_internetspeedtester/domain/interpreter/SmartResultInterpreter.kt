package com.sk.wifi_internetspeedtester.domain.interpreter

import com.sk.wifi_internetspeedtester.domain.model.ActivityCapability
import com.sk.wifi_internetspeedtester.domain.model.NetworkType
import com.sk.wifi_internetspeedtester.domain.model.RatingLevel
import com.sk.wifi_internetspeedtester.domain.model.SmartExplanation
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestResult
import java.util.Locale

object SmartResultInterpreter {

    fun interpret(result: SpeedTestResult): SmartExplanation {
        val dl = result.downloadMbps
        val ul = result.uploadMbps
        val ping = result.pingMs
        val jitter = result.jitterMs

        val headline: String
        val summary: String

        when {
            dl >= 100.0 && ping <= 30.0 -> {
                headline = "Blazing Fast Connection"
                summary = "Your internet speed is exceptional. Perfect for multi-device 4K streaming, ultra-low-latency gaming, and instant file downloads."
            }
            dl >= 50.0 && ping <= 50.0 -> {
                headline = "Fast & Reliable Speed"
                summary = "Your connection easily handles 4K video streaming, high-definition video calls, and smooth online activities."
            }
            dl >= 20.0 -> {
                headline = "Good Internet Connection"
                summary = "Your speed is sufficient for HD video streaming, smooth browsing, and standard video calling."
            }
            dl >= 5.0 -> {
                headline = "Moderate Speed"
                summary = "Your internet works well for social media, web browsing, and 720p streaming, but heavy downloads or multiple devices may feel slow."
            }
            else -> {
                headline = "Slow Internet Connection"
                summary = "Your download speed is limited. Video streams may buffer and web pages may take longer to load."
            }
        }

        // Streaming capability
        val streamingRating = when {
            dl >= 25.0 -> RatingLevel.EXCELLENT
            dl >= 10.0 -> RatingLevel.GOOD
            dl >= 3.0 -> RatingLevel.FAIR
            else -> RatingLevel.POOR
        }
        val streamingDesc = when (streamingRating) {
            RatingLevel.EXCELLENT -> "Smooth 4K Ultra HD streaming across multiple screens."
            RatingLevel.GOOD -> "Great for 1080p Full HD video streaming."
            RatingLevel.FAIR -> "Standard definition (SD) streaming works well."
            RatingLevel.POOR -> "Frequent buffering expected during video playback."
        }

        // Video calls capability
        val videoCallRating = when {
            dl >= 8.0 && ul >= 3.0 && ping <= 80.0 -> RatingLevel.EXCELLENT
            dl >= 3.0 && ul >= 1.0 && ping <= 120.0 -> RatingLevel.GOOD
            dl >= 1.5 && ul >= 0.5 -> RatingLevel.FAIR
            else -> RatingLevel.POOR
        }
        val videoCallDesc = when (videoCallRating) {
            RatingLevel.EXCELLENT -> "Crystal clear HD video calls and effortless screen sharing."
            RatingLevel.GOOD -> "Reliable video calls with clear audio and video."
            RatingLevel.FAIR -> "Acceptable for standard audio/video calls."
            RatingLevel.POOR -> "Video calls may freeze, lag, or drop out."
        }

        // Online gaming capability
        val gamingRating = when {
            ping <= 30.0 && jitter <= 10.0 -> RatingLevel.EXCELLENT
            ping <= 60.0 && jitter <= 20.0 -> RatingLevel.GOOD
            ping <= 100.0 -> RatingLevel.FAIR
            else -> RatingLevel.POOR
        }
        val gamingDesc = when (gamingRating) {
            RatingLevel.EXCELLENT -> "Ultra-responsive ping ($ping ms) for competitive online gaming."
            RatingLevel.GOOD -> "Smooth online multiplayer gaming experience."
            RatingLevel.FAIR -> "Playable, though minor delay may occur."
            RatingLevel.POOR -> "High ping ($ping ms) will cause noticeable lag."
        }

        // Web Browsing capability
        val browsingRating = when {
            dl >= 30.0 -> RatingLevel.EXCELLENT
            dl >= 10.0 -> RatingLevel.GOOD
            dl >= 2.0 -> RatingLevel.FAIR
            else -> RatingLevel.POOR
        }
        val browsingDesc = when (browsingRating) {
            RatingLevel.EXCELLENT -> "Web pages and app content load almost instantly."
            RatingLevel.GOOD -> "Fast webpage loading and quick app responsiveness."
            RatingLevel.FAIR -> "Normal web browsing with minor loading times."
            RatingLevel.POOR -> "Pages and images will take noticeable time to open."
        }

        val capabilities = listOf(
            ActivityCapability("4K/HD Streaming", streamingRating, streamingDesc, "streaming"),
            ActivityCapability("Video Calling", videoCallRating, videoCallDesc, "videocall"),
            ActivityCapability("Online Gaming", gamingRating, gamingDesc, "gaming"),
            ActivityCapability("Web Browsing", browsingRating, browsingDesc, "browsing")
        )

        val tips = mutableListOf<String>()
        if (result.packetLossPercent > 1.0) {
            tips.add("Packet loss detected (${String.format(Locale.US, "%.1f", result.packetLossPercent)}%). This can cause stuttering in live video calls and gaming.")
        }
        if (ping > 80.0) {
            tips.add("Your latency (ping) is slightly high ($ping ms). Moving closer to your Wi-Fi router can improve response times.")
        }
        if (jitter > 20.0) {
            tips.add("High jitter ($jitter ms) indicates network fluctuation. Check if other devices on the network are downloading large files.")
        }
        if (dl < 10.0 && result.networkType == NetworkType.WIFI) {
            tips.add("Restarting your Wi-Fi router can often fix temporary bandwidth bottlenecks.")
        }
        if (tips.isEmpty()) {
            tips.add("Your network metrics look balanced and healthy.")
        }

        return SmartExplanation(
            headline = headline,
            summary = summary,
            capabilities = capabilities,
            troubleshootingTips = tips
        )
    }

    fun formatSpeed(mbps: Double): String {
        return if (mbps >= 10.0) {
            String.format(Locale.US, "%.1f", mbps)
        } else {
            String.format(Locale.US, "%.2f", mbps)
        }
    }

    fun formatPing(ms: Double): String {
        return String.format(Locale.US, "%.0f", ms)
    }

    fun calculateOverallQuality(dl: Double, ul: Double, ping: Double, packetLoss: Double): RatingLevel {
        return when {
            dl >= 50.0 && ul >= 15.0 && ping <= 40.0 && packetLoss <= 1.0 -> RatingLevel.EXCELLENT
            dl >= 20.0 && ul >= 5.0 && ping <= 80.0 && packetLoss <= 3.0 -> RatingLevel.GOOD
            dl >= 5.0 && ul >= 1.0 && ping <= 150.0 && packetLoss <= 5.0 -> RatingLevel.FAIR
            else -> RatingLevel.POOR
        }
    }
}
