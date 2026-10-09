package com.sk.wifi_internetspeedtester.data.network

import com.sk.wifi_internetspeedtester.domain.model.SpeedTestServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Production Server Selector for speed test endpoints.
 *
 * Endpoint Validation & Audit Notes:
 * - Cloudflare Speed CDN (speed.cloudflare.com) is used as the primary production speed test provider.
 *   It features Anycast routing to 300+ global edge locations, uncompressed byte streams,
 *   and high-capacity upload discarding endpoints (__up).
 * - HTTPBin (httpbin.org) was audited and REMOVED:
 *   HTTPBin is a REST API debug service with strict request body limits (~1MB) and JSON echoing.
 *   It rate-limits client IP addresses and fails on high-speed multi-megabyte uploads.
 * - Hetzner (speed.hetzner.de) was audited and REMOVED from default selection:
 *   Hetzner provides download files (100MB.bin) but lacks a matching public HTTP POST upload endpoint,
 *   which would result in asymmetrical tests against different servers.
 */
object ServerSelector {

    val defaultServers = listOf(
        SpeedTestServer(
            id = "cloudflare_anycast_primary",
            name = "Optimal High-Speed Node",
            location = "Global Low-Latency Edge",
            host = "speed.cloudflare.com",
            pingUrl = "https://speed.cloudflare.com/__down?bytes=100",
            downloadUrl = "https://speed.cloudflare.com/__down?bytes=50000000",
            uploadUrl = "https://speed.cloudflare.com/__up"
        ),
        SpeedTestServer(
            id = "cloudflare_anycast_secondary",
            name = "Optimal Low-Latency Node",
            location = "Global Low-Latency Edge",
            host = "speed.cloudflare.com",
            pingUrl = "https://speed.cloudflare.com/__down?bytes=1000",
            downloadUrl = "https://speed.cloudflare.com/__down?bytes=25000000",
            uploadUrl = "https://speed.cloudflare.com/__up"
        )
    )

    suspend fun selectBestServer(servers: List<SpeedTestServer> = defaultServers): SpeedTestServer = withContext(Dispatchers.IO) {
        if (servers.isEmpty()) return@withContext defaultServers.first()

        val results = servers.map { server ->
            async {
                val ping = measureServerPing(server.pingUrl)
                Pair(server, ping)
            }
        }.awaitAll()

        val best = results.filter { it.second > 0 }.minByOrNull { it.second }
        best?.first ?: servers.first()
    }

    private fun measureServerPing(pingUrl: String): Long {
        return try {
            val startTime = System.currentTimeMillis()
            val url = URL("$pingUrl&nocache=${System.currentTimeMillis()}")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            connection.connect()
            val responseCode = connection.responseCode
            connection.disconnect()
            if (responseCode in 200..399) {
                System.currentTimeMillis() - startTime
            } else -1L
        } catch (_: Exception) {
            -1L
        }
    }
}
