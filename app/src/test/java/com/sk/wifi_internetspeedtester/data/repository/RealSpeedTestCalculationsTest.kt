package com.sk.wifi_internetspeedtester.data.repository

import com.sk.wifi_internetspeedtester.domain.model.NetworkStatus
import com.sk.wifi_internetspeedtester.domain.model.NetworkType
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestStage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealSpeedTestCalculationsTest {

    @Test
    fun testMbpsCalculation() {
        val bytes = 12_500_000L
        val seconds = 2.0
        val mbps = RealSpeedTestRepository.calculateMbps(bytes, seconds)
        assertEquals(50.0, mbps, 0.001)
    }

    @Test
    fun testMbpsZeroSecondsHandling() {
        val mbps = RealSpeedTestRepository.calculateMbps(1000L, 0.0)
        assertEquals(0.0, mbps, 0.0)
    }

    @Test
    fun testJitterCalculation() {
        val samples = listOf(20.0, 30.0, 25.0)
        val jitter = RealSpeedTestRepository.calculateJitter(samples)
        assertEquals(7.5, jitter, 0.001)
    }

    @Test
    fun testJitterSingleSample() {
        val jitter = RealSpeedTestRepository.calculateJitter(listOf(20.0))
        assertEquals(0.0, jitter, 0.0)
    }

    @Test
    fun testStabilityCalculation() {
        val stability = RealSpeedTestRepository.calculateStability(4.0, 0.0)
        assertEquals(94.0, stability, 0.01)
    }

    @Test
    fun testDisconnectedNetworkErrorState() {
        val repository = RealSpeedTestRepository()
        val disconnectedStatus = NetworkStatus(
            type = NetworkType.DISCONNECTED,
            isConnected = false
        )

        repository.startTest(disconnectedStatus)

        val state = repository.speedTestState.value
        assertEquals(SpeedTestStage.ERROR, state.stage)
        assertTrue(state.errorMessage?.contains("No internet connection") == true)
    }

    @Test
    fun testCancellationState() = runBlocking {
        val repository = RealSpeedTestRepository()
        repository.cancelTest()

        val state = repository.speedTestState.value
        assertEquals(SpeedTestStage.IDLE, state.stage)
    }
}
