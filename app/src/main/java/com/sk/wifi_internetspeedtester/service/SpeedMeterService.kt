package com.sk.wifi_internetspeedtester.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sk.wifi_internetspeedtester.MainActivity
import com.sk.wifi_internetspeedtester.R
import com.sk.wifi_internetspeedtester.data.repository.DataUsageRepository
import com.sk.wifi_internetspeedtester.data.repository.NetworkRepository
import com.sk.wifi_internetspeedtester.domain.model.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpeedMeterService : Service() {

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(LiveTrafficData()))

        scope.launch {
            while (isActive) {
                val data = TrafficStatsMonitor.sampleLiveTraffic()
                _liveSpeedFlow.value = data
                updateNotification(data)
                delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Speed Meter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time internet download and upload speed in status bar"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(data: LiveTrafficData) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(data))
    }

    private fun buildNotification(data: LiveTrafficData): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dataUsageRepo = DataUsageRepository(this)
        val todayUsage = dataUsageRepo.getDailyHistory(1).firstOrNull()
        val mobileMB = TrafficStatsMonitor.formatBytes(todayUsage?.mobileBytes ?: 0L)
        val wifiMB = TrafficStatsMonitor.formatBytes(todayUsage?.wifiBytes ?: 0L)

        val netRepo = NetworkRepository(this)
        val currentStatus = netRepo.getCurrentNetworkStatus()
        val signalPct = if (currentStatus.type == NetworkType.WIFI) "${currentStatus.signalPercentage}%" else "100%"

        val titleText = "Speed: ${data.formattedDownloadBytesRate}   Signal $signalPct"
        val contentText = "Mobile: $mobileMB   WiFi: $wifiMB"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setSubText(data.formattedDownloadBytesRate)
            .setTicker(titleText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "live_speed_meter_channel"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning = false
            private set

        private val _liveSpeedFlow = MutableStateFlow(LiveTrafficData())
        val liveSpeedFlow: StateFlow<LiveTrafficData> = _liveSpeedFlow.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, SpeedMeterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SpeedMeterService::class.java)
            context.stopService(intent)
            isRunning = false
        }
    }
}
