package com.sk.wifi_internetspeedtester.data.repository

import android.content.Context
import com.sk.wifi_internetspeedtester.data.network.ServerSelector
import com.sk.wifi_internetspeedtester.domain.model.NetworkStatus
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestResult
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestServer
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestStage
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Arrays
import kotlin.math.abs

import com.sk.wifi_internetspeedtester.domain.interpreter.SmartResultInterpreter
import com.sk.wifi_internetspeedtester.domain.model.NetworkType
import com.sk.wifi_internetspeedtester.util.NetworkDetailsProvider

class RealSpeedTestRepository(
    private val context: Context? = null,
    private val serverSelector: ServerSelector = ServerSelector
) : SpeedTestRepository {

    private val _speedTestState = MutableStateFlow(SpeedTestState())
    override val speedTestState: StateFlow<SpeedTestState> = _speedTestState.asStateFlow()

    private val _lastResult = MutableStateFlow<SpeedTestResult?>(null)
    override val lastResult: StateFlow<SpeedTestResult?> = _lastResult.asStateFlow()

    private var testJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var activeConnection: HttpURLConnection? = null

    override fun startTest(networkStatus: NetworkStatus) {
        if (!networkStatus.isConnected) {
            _speedTestState.value = SpeedTestState(
                stage = SpeedTestStage.ERROR,
                errorMessage = "No internet connection detected. Please connect to Wi-Fi or mobile data."
            )
            return
        }

        cancelTest()

        testJob = scope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                // Stage 1: CONNECTING & Server Selection
                _speedTestState.value = SpeedTestState(
                    stage = SpeedTestStage.CONNECTING,
                    progress = 0.05f,
                    serverName = "Selecting optimal server...",
                    serverLocation = "Locating server"
                )

                val selectedServer = serverSelector.selectBestServer()
                ensureActive()

                _speedTestState.value = _speedTestState.value.copy(
                    progress = 0.10f,
                    serverName = selectedServer.name,
                    serverLocation = selectedServer.location
                )

                // Stage 2: PING, JITTER & PACKET LOSS
                _speedTestState.value = _speedTestState.value.copy(
                    stage = SpeedTestStage.PING,
                    progress = 0.15f
                )

                val pingResult = measurePingJitterAndPacketLoss(selectedServer.pingUrl)
                ensureActive()

                _speedTestState.value = _speedTestState.value.copy(
                    progress = 0.30f,
                    pingMs = pingResult.avgPingMs,
                    jitterMs = pingResult.jitterMs,
                    packetLossPercent = pingResult.packetLossPercent
                )

                // Stage 3: REAL DOWNLOAD TEST
                _speedTestState.value = _speedTestState.value.copy(
                    stage = SpeedTestStage.DOWNLOAD,
                    progress = 0.35f
                )

                val downloadMbps = measureDownloadSpeed(selectedServer.downloadUrl)
                ensureActive()

                _speedTestState.value = _speedTestState.value.copy(
                    progress = 0.65f,
                    downloadMbps = downloadMbps
                )

                // Stage 4: REAL UPLOAD TEST
                _speedTestState.value = _speedTestState.value.copy(
                    stage = SpeedTestStage.UPLOAD,
                    progress = 0.70f,
                    currentSpeedMbps = 0.0
                )

                val uploadMbps = measureUploadSpeed(selectedServer.uploadUrl)
                ensureActive()

                // Stage 5: FINALIZING & COMPLETED
                _speedTestState.value = _speedTestState.value.copy(
                    stage = SpeedTestStage.FINALIZING,
                    progress = 0.95f,
                    uploadMbps = uploadMbps
                )

                val totalDurationSec = (System.currentTimeMillis() - startTime) / 1000.0
                val stabilityScore = calculateStability(
                    pingResult.jitterMs,
                    pingResult.packetLossPercent
                )

                val netTypeLabel = context?.let { NetworkDetailsProvider.getNetworkTypeLabel(it, networkStatus.type) } ?: if (networkStatus.type == NetworkType.WIFI) "Wi-Fi" else "Mobile Data"
                val ispName = context?.let { NetworkDetailsProvider.getIspName(it) }
                val locationName = context?.let { NetworkDetailsProvider.getLocationName(it) }
                val quality = SmartResultInterpreter.calculateOverallQuality(downloadMbps, uploadMbps, pingResult.avgPingMs, pingResult.packetLossPercent)

                val finalResult = SpeedTestResult(
                    downloadMbps = downloadMbps,
                    uploadMbps = uploadMbps,
                    pingMs = pingResult.avgPingMs,
                    jitterMs = pingResult.jitterMs,
                    packetLossPercent = pingResult.packetLossPercent,
                    stabilityPercent = stabilityScore,
                    durationSeconds = totalDurationSec,
                    networkType = networkStatus.type,
                    networkName = networkStatus.ssid ?: "Connected Network",
                    serverName = selectedServer.name,
                    networkTypeLabel = netTypeLabel,
                    ispName = ispName,
                    locationName = locationName,
                    overallQuality = quality
                )

                _lastResult.value = finalResult
                _speedTestState.value = SpeedTestState(
                    stage = SpeedTestStage.COMPLETED,
                    progress = 1.0f,
                    downloadMbps = downloadMbps,
                    uploadMbps = uploadMbps,
                    pingMs = pingResult.avgPingMs,
                    jitterMs = pingResult.jitterMs,
                    packetLossPercent = pingResult.packetLossPercent,
                    serverName = selectedServer.name,
                    serverLocation = selectedServer.location
                )

            } catch (e: Exception) {
                if (e is CancellationException) {
                    _speedTestState.value = SpeedTestState(stage = SpeedTestStage.CANCELLED)
                    throw e
                }
                val userErrorMsg = mapExceptionToUserMessage(e)
                _speedTestState.value = SpeedTestState(
                    stage = SpeedTestStage.ERROR,
                    errorMessage = userErrorMsg
                )
            } finally {
                closeActiveConnection()
            }
        }
    }

    override fun cancelTest() {
        closeActiveConnection()
        testJob?.cancel()
        _speedTestState.value = SpeedTestState(stage = SpeedTestStage.IDLE)
    }

    private data class PingMetrics(
        val avgPingMs: Double,
        val jitterMs: Double,
        val packetLossPercent: Double
    )

    private suspend fun measurePingJitterAndPacketLoss(pingUrl: String): PingMetrics = withContext(Dispatchers.IO) {
        val totalProbes = 8
        val samples = mutableListOf<Double>()
        var failedProbes = 0

        for (i in 1..totalProbes) {
            val startTime = System.currentTimeMillis()
            val success = try {
                val urlWithNocache = if (pingUrl.contains("?")) "$pingUrl&nocache=${System.currentTimeMillis()}_$i" else "$pingUrl?nocache=${System.currentTimeMillis()}_$i"
                val url = URL(urlWithNocache)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2000
                    readTimeout = 2000
                    useCaches = false
                    setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                    setRequestProperty("Pragma", "no-cache")
                }
                activeConnection = conn
                conn.connect()
                val code = conn.responseCode
                conn.disconnect()
                code in 200..399
            } catch (_: Exception) {
                false
            } finally {
                closeActiveConnection()
            }

            if (success) {
                val elapsed = (System.currentTimeMillis() - startTime).toDouble()
                samples.add(elapsed)
            } else {
                failedProbes++
            }

            val currentProgress = 0.15f + (i.toFloat() / totalProbes) * 0.15f
            val currentAvgPing = if (samples.isNotEmpty()) samples.average() else 0.0
            _speedTestState.value = _speedTestState.value.copy(
                progress = currentProgress,
                pingMs = currentAvgPing
            )
        }

        val avgPing = if (samples.isNotEmpty()) samples.average() else 0.0
        val jitter = calculateJitter(samples)
        val packetLoss = (failedProbes.toDouble() / totalProbes) * 100.0

        PingMetrics(
            avgPingMs = avgPing,
            jitterMs = jitter,
            packetLossPercent = packetLoss
        )
    }

    private suspend fun measureDownloadSpeed(downloadUrl: String): Double = withContext(Dispatchers.IO) {
        val downloadUrlWithNocache = if (downloadUrl.contains("?")) "$downloadUrl&nocache=${System.currentTimeMillis()}" else "$downloadUrl?nocache=${System.currentTimeMillis()}"
        val url = URL(downloadUrlWithNocache)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            useCaches = false
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            setRequestProperty("Pragma", "no-cache")
        }
        activeConnection = conn

        conn.connect()
        val responseCode = conn.responseCode
        if (responseCode !in 200..399) {
            throw IOException("Download server responded with HTTP $responseCode")
        }

        val inputStream: InputStream = conn.inputStream
        val buffer = ByteArray(32 * 1024)
        var totalBytesRead = 0L
        val testStartTime = System.currentTimeMillis()
        val maxTestDurationMs = 7000L

        var lastUpdateMs = testStartTime
        var finalSpeedMbps = 0.0

        try {
            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                totalBytesRead += bytesRead
                val now = System.currentTimeMillis()
                val elapsedSec = (now - testStartTime) / 1000.0

                if (elapsedSec > 0.05) {
                    finalSpeedMbps = calculateMbps(totalBytesRead, elapsedSec)
                }

                if (now - lastUpdateMs >= 100) {
                    lastUpdateMs = now
                    val progressRatio = ((now - testStartTime).toFloat() / maxTestDurationMs).coerceIn(0f, 1f)
                    _speedTestState.value = _speedTestState.value.copy(
                        progress = 0.35f + (progressRatio * 0.30f),
                        currentSpeedMbps = finalSpeedMbps,
                        downloadMbps = finalSpeedMbps
                    )
                }

                if (now - testStartTime >= maxTestDurationMs) {
                    break
                }
            }
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
            closeActiveConnection()
        }

        finalSpeedMbps
    }

    private suspend fun measureUploadSpeed(uploadUrl: String): Double = withContext(Dispatchers.IO) {
        val url = URL(uploadUrl)
        val payloadChunk = ByteArray(32 * 1024)
        Arrays.fill(payloadChunk, 0x55.toByte())

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 5000
            useCaches = false
            doOutput = true
            setChunkedStreamingMode(32 * 1024)
            setRequestProperty("Content-Type", "application/octet-stream")
        }
        activeConnection = conn

        conn.connect()
        val outputStream: OutputStream = conn.outputStream
        var totalBytesSent = 0L
        val testStartTime = System.currentTimeMillis()
        val maxTestDurationMs = 6000L

        var lastUpdateMs = testStartTime
        var finalSpeedMbps = 0.0

        try {
            while (true) {
                outputStream.write(payloadChunk)
                totalBytesSent += payloadChunk.size

                val now = System.currentTimeMillis()
                val elapsedSec = (now - testStartTime) / 1000.0

                if (elapsedSec > 0.05) {
                    finalSpeedMbps = calculateMbps(totalBytesSent, elapsedSec)
                }

                if (now - lastUpdateMs >= 100) {
                    lastUpdateMs = now
                    val progressRatio = ((now - testStartTime).toFloat() / maxTestDurationMs).coerceIn(0f, 1f)
                    _speedTestState.value = _speedTestState.value.copy(
                        progress = 0.70f + (progressRatio * 0.25f),
                        currentSpeedMbps = finalSpeedMbps,
                        uploadMbps = finalSpeedMbps
                    )
                }

                if (now - testStartTime >= maxTestDurationMs) {
                    break
                }
            }
            outputStream.flush()
        } catch (e: Exception) {
            // Even if server closes stream early after receiving data, calculate sent rate
            if (totalBytesSent == 0L) throw e
        } finally {
            try { outputStream.close() } catch (_: Exception) {}
            closeActiveConnection()
        }

        finalSpeedMbps
    }

    private fun closeActiveConnection() {
        try {
            activeConnection?.disconnect()
        } catch (_: Exception) {}
        activeConnection = null
    }

    private fun mapExceptionToUserMessage(e: Exception): String {
        return when (e) {
            is UnknownHostException -> "No internet connection available or server host unresolvable."
            is SocketTimeoutException -> "Speed test connection timed out. Check network stability."
            is ConnectException -> "Unable to connect to test server. Network may be restricted."
            else -> e.localizedMessage ?: "Speed test encountered a network error."
        }
    }

    companion object {
        fun calculateMbps(bytes: Long, seconds: Double): Double {
            if (seconds <= 0) return 0.0
            val bits = bytes * 8.0
            val megabits = bits / 1_000_000.0
            return megabits / seconds
        }

        fun calculateJitter(samples: List<Double>): Double {
            if (samples.size < 2) return 0.0
            var diffSum = 0.0
            for (i in 1 until samples.size) {
                diffSum += abs(samples[i] - samples[i - 1])
            }
            return diffSum / (samples.size - 1)
        }

        fun calculateStability(jitterMs: Double, packetLossPercent: Double): Double {
            val score = 100.0 - (packetLossPercent * 3.0) - (jitterMs.coerceAtMost(30.0) * 1.5)
            return score.coerceIn(40.0, 99.9)
        }
    }
}
