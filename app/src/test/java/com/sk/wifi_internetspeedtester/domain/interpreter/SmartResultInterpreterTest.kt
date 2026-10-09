package com.sk.wifi_internetspeedtester.domain.interpreter

import com.sk.wifi_internetspeedtester.domain.model.NetworkType
import com.sk.wifi_internetspeedtester.domain.model.RatingLevel
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartResultInterpreterTest {

    @Test
    fun testFastConnectionInterpretation() {
        val result = SpeedTestResult(
            downloadMbps = 150.0,
            uploadMbps = 45.0,
            pingMs = 15.0,
            jitterMs = 3.0,
            stabilityPercent = 99.0,
            durationSeconds = 8.5,
            networkType = NetworkType.WIFI,
            networkName = "Home_WiFi_5G",
            serverName = "FastNet Server"
        )

        val explanation = SmartResultInterpreter.interpret(result)

        assertEquals("Blazing Fast Connection", explanation.headline)
        assertTrue(explanation.summary.contains("exceptional"))

        val streamingCap = explanation.capabilities.first { it.name.contains("4K") }
        assertEquals(RatingLevel.EXCELLENT, streamingCap.rating)

        val gamingCap = explanation.capabilities.first { it.name.contains("Gaming") }
        assertEquals(RatingLevel.EXCELLENT, gamingCap.rating)
    }

    @Test
    fun testSlowConnectionTroubleshootingTips() {
        val result = SpeedTestResult(
            downloadMbps = 4.0,
            uploadMbps = 0.8,
            pingMs = 110.0,
            jitterMs = 28.0,
            stabilityPercent = 85.0,
            durationSeconds = 8.5,
            networkType = NetworkType.WIFI,
            networkName = "WeakWiFi",
            serverName = "Remote Server"
        )

        val explanation = SmartResultInterpreter.interpret(result)

        assertEquals("Slow Internet Connection", explanation.headline)
        assertTrue(explanation.troubleshootingTips.isNotEmpty())

        val hasPingTip = explanation.troubleshootingTips.any { it.contains("latency") || it.contains("ping") }
        assertTrue("Should include high ping tip", hasPingTip)
    }

    @Test
    fun testFormattingUtilities() {
        val speedFormatted = SmartResultInterpreter.formatSpeed(85.43)
        assertEquals("85.4", speedFormatted)

        val lowSpeedFormatted = SmartResultInterpreter.formatSpeed(4.567)
        assertEquals("4.57", lowSpeedFormatted)

        val pingFormatted = SmartResultInterpreter.formatPing(18.9)
        assertEquals("19", pingFormatted)
    }
}
