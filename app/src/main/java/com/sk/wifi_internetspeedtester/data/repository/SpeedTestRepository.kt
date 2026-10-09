package com.sk.wifi_internetspeedtester.data.repository

import com.sk.wifi_internetspeedtester.domain.model.NetworkStatus
import com.sk.wifi_internetspeedtester.domain.model.NetworkType
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestResult
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestStage
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

interface SpeedTestRepository {
    val speedTestState: StateFlow<SpeedTestState>
    val lastResult: StateFlow<SpeedTestResult?>
    fun startTest(networkStatus: NetworkStatus)
    fun cancelTest()
}

class MockSpeedTestRepository : SpeedTestRepository {

    private val _speedTestState = MutableStateFlow(SpeedTestState())
    override val speedTestState: StateFlow<SpeedTestState> = _speedTestState.asStateFlow()

    private val _lastResult = MutableStateFlow<SpeedTestResult?>(null)
    override val lastResult: StateFlow<SpeedTestResult?> = _lastResult.asStateFlow()

    private var testJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun startTest(networkStatus: NetworkStatus) {
        testJob?.cancel()

        testJob = scope.launch {
            try {
                val targetDownload = when (networkStatus.type) {
                    NetworkType.WIFI -> Random.nextDouble(65.0, 125.0)
                    NetworkType.CELLULAR -> Random.nextDouble(30.0, 75.0)
                    NetworkType.ETHERNET -> Random.nextDouble(150.0, 300.0)
                    NetworkType.DISCONNECTED -> Random.nextDouble(5.0, 15.0)
                }

                val targetUpload = when (networkStatus.type) {
                    NetworkType.WIFI -> targetDownload * Random.nextDouble(0.3, 0.6)
                    NetworkType.CELLULAR -> targetDownload * Random.nextDouble(0.2, 0.4)
                    NetworkType.ETHERNET -> targetDownload * Random.nextDouble(0.8, 0.95)
                    NetworkType.DISCONNECTED -> targetDownload * 0.2
                }

                val targetPing = when (networkStatus.type) {
                    NetworkType.WIFI -> Random.nextDouble(12.0, 28.0)
                    NetworkType.CELLULAR -> Random.nextDouble(35.0, 65.0)
                    NetworkType.ETHERNET -> Random.nextDouble(5.0, 12.0)
                    NetworkType.DISCONNECTED -> Random.nextDouble(80.0, 180.0)
                }

                val targetJitter = Random.nextDouble(1.5, 6.0)

                val serverName = if (networkStatus.type == NetworkType.WIFI) {
                    "FastNet Cloud Node (#${Random.nextInt(100, 999)})"
                } else {
                    "Edge Telecom Server"
                }

                // 1. CONNECTING
                _speedTestState.value = SpeedTestState(
                    stage = SpeedTestStage.CONNECTING,
                    progress = 0.05f,
                    serverName = serverName,
                    serverLocation = "Nearest Low-Latency Node"
                )
                delay(700)

                // 2. PING & JITTER
                _speedTestState.value = _speedTestState.value.copy(
                    stage = SpeedTestStage.PING,
                    progress = 0.15f
                )

                val pingSteps = 6
                var simulatedPing = targetPing
                for (i in 1..pingSteps) {
                    simulatedPing = targetPing + Random.nextDouble(-2.0, 2.0)
                    _speedTestState.value = _speedTestState.value.copy(
                        progress = 0.15f + (i.toFloat() / pingSteps) * 0.15f,
                        pingMs = simulatedPing,
                        jitterMs = targetJitter + Random.nextDouble(-0.5, 0.5)
                    )
                    delay(150)
                }

                val finalPing = simulatedPing
                val finalJitter = targetJitter

                // 3. DOWNLOAD TEST
                _speedTestState.value = _speedTestState.value.copy(
                    stage = SpeedTestStage.DOWNLOAD,
                    progress = 0.30f
                )

                val downloadSteps = 25
                var currentDl = 0.0
                for (i in 1..downloadSteps) {
                    val ratio = i.toDouble() / downloadSteps
                    val curve = sin(ratio * Math.PI / 2)
                    val noise = Random.nextDouble(-0.05, 0.05)
                    currentDl = (targetDownload * curve * (1 + noise)).coerceAtLeast(0.5)

                    _speedTestState.value = _speedTestState.value.copy(
                        progress = 0.30f + (ratio.toFloat() * 0.35f),
                        currentSpeedMbps = currentDl,
                        downloadMbps = currentDl
                    )
                    delay(120)
                }
                val finalDownload = currentDl

                // 4. UPLOAD TEST
                _speedTestState.value = _speedTestState.value.copy(
                    stage = SpeedTestStage.UPLOAD,
                    progress = 0.65f,
                    currentSpeedMbps = 0.0
                )

                val uploadSteps = 20
                var currentUl = 0.0
                for (i in 1..uploadSteps) {
                    val ratio = i.toDouble() / uploadSteps
                    val curve = sin(ratio * Math.PI / 2)
                    val noise = Random.nextDouble(-0.04, 0.04)
                    currentUl = (targetUpload * curve * (1 + noise)).coerceAtLeast(0.2)

                    _speedTestState.value = _speedTestState.value.copy(
                        progress = 0.65f + (ratio.toFloat() * 0.30f),
                        currentSpeedMbps = currentUl,
                        uploadMbps = currentUl
                    )
                    delay(120)
                }
                val finalUpload = currentUl

                // 5. COMPLETED
                val finalResult = SpeedTestResult(
                    downloadMbps = finalDownload,
                    uploadMbps = finalUpload,
                    pingMs = finalPing,
                    jitterMs = finalJitter,
                    stabilityPercent = Random.nextDouble(96.0, 99.8),
                    durationSeconds = 8.5,
                    networkType = networkStatus.type,
                    networkName = networkStatus.ssid ?: "Connected Network",
                    serverName = serverName
                )

                _lastResult.value = finalResult
                _speedTestState.value = SpeedTestState(
                    stage = SpeedTestStage.COMPLETED,
                    progress = 1.0f,
                    downloadMbps = finalDownload,
                    uploadMbps = finalUpload,
                    pingMs = finalPing,
                    jitterMs = finalJitter,
                    serverName = serverName
                )

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _speedTestState.value = SpeedTestState(
                    stage = SpeedTestStage.ERROR,
                    errorMessage = e.localizedMessage ?: "Speed test encountered an error."
                )
            }
        }
    }

    override fun cancelTest() {
        testJob?.cancel()
        _speedTestState.value = SpeedTestState(stage = SpeedTestStage.IDLE)
    }
}
