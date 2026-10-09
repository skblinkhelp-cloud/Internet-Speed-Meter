package com.sk.wifi_internetspeedtester.domain.model

data class DailyDataUsage(
    val dateString: String,
    val mobileBytes: Long,
    val wifiBytes: Long
) {
    val totalBytes: Long
        get() = mobileBytes + wifiBytes
}

data class MonthlyUsageSummary(
    val monthString: String = "This Month",
    val mobileBytes: Long,
    val wifiBytes: Long
) {
    val totalBytes: Long
        get() = mobileBytes + wifiBytes
}
