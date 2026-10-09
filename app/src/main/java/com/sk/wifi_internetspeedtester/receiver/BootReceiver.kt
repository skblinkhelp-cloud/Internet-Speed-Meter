package com.sk.wifi_internetspeedtester.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sk.wifi_internetspeedtester.service.SpeedMeterService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val startOnBoot = prefs.getBoolean("start_on_boot", true)
            val serviceEnabled = prefs.getBoolean("notification_service_enabled", true)

            if (startOnBoot && serviceEnabled) {
                SpeedMeterService.start(context)
            }
        }
    }
}
