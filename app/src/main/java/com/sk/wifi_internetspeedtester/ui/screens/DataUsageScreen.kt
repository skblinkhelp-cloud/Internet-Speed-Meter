package com.sk.wifi_internetspeedtester.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sk.wifi_internetspeedtester.domain.model.DailyDataUsage
import com.sk.wifi_internetspeedtester.domain.model.MonthlyUsageSummary
import com.sk.wifi_internetspeedtester.service.TrafficStatsMonitor

@Composable
fun DataUsageScreen(
    dailyHistory: List<DailyDataUsage>,
    monthlySummary: MonthlyUsageSummary
) {
    val scrollState = rememberScrollState()

    val headerBlue = Color(0xFF3B82F6)
    val rowAltBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val tableBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Screen Title
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
            Text(
                text = "Data Usage Log",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Daily & Monthly Internet Data Tracking",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )
        }

        // Summary Cards (Today & This Month)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val todayUsage = dailyHistory.firstOrNull() ?: DailyDataUsage("Today", 0L, 0L)

            SummaryBox(
                title = "Today",
                mobileText = TrafficStatsMonitor.formatBytes(todayUsage.mobileBytes),
                wifiText = TrafficStatsMonitor.formatBytes(todayUsage.wifiBytes),
                totalText = TrafficStatsMonitor.formatBytes(todayUsage.totalBytes),
                accentColor = headerBlue,
                modifier = Modifier.weight(1f)
            )

            SummaryBox(
                title = "This Month",
                mobileText = TrafficStatsMonitor.formatBytes(monthlySummary.mobileBytes),
                wifiText = TrafficStatsMonitor.formatBytes(monthlySummary.wifiBytes),
                totalText = TrafficStatsMonitor.formatBytes(monthlySummary.totalBytes),
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Data Usage Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tableBorderColor, RoundedCornerShape(16.dp))
            ) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBlue)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(text = "Date", weight = 1.3f, isHeader = true, color = Color.White)
                    TableCell(text = "Mobile", weight = 1f, isHeader = true, color = Color.White)
                    TableCell(text = "WiFi", weight = 1f, isHeader = true, color = Color.White)
                    TableCell(text = "Total", weight = 1.1f, isHeader = true, color = Color.White)
                }

                HorizontalDivider(color = tableBorderColor)

                // Table Rows
                dailyHistory.forEachIndexed { index, item ->
                    val isEven = index % 2 == 0
                    val isToday = index == 0
                    val rowBg = if (isToday) headerBlue.copy(alpha = 0.12f) else if (isEven) Color.Transparent else rowAltBg

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell(
                            text = item.dateString,
                            weight = 1.3f,
                            isBold = isToday,
                            color = if (isToday) headerBlue else MaterialTheme.colorScheme.onSurface
                        )
                        TableCell(
                            text = TrafficStatsMonitor.formatBytes(item.mobileBytes),
                            weight = 1f,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TableCell(
                            text = TrafficStatsMonitor.formatBytes(item.wifiBytes),
                            weight = 1f,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TableCell(
                            text = TrafficStatsMonitor.formatBytes(item.totalBytes),
                            weight = 1.1f,
                            isBold = true,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = tableBorderColor)
                }

                // Monthly Summary Footer Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBlue.copy(alpha = 0.2f))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(text = monthlySummary.monthString, weight = 1.3f, isHeader = true, color = headerBlue)
                    TableCell(text = TrafficStatsMonitor.formatBytes(monthlySummary.mobileBytes), weight = 1f, isBold = true, color = headerBlue)
                    TableCell(text = TrafficStatsMonitor.formatBytes(monthlySummary.wifiBytes), weight = 1f, isBold = true, color = headerBlue)
                    TableCell(text = TrafficStatsMonitor.formatBytes(monthlySummary.totalBytes), weight = 1.1f, isBold = true, color = headerBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryBox(
    title: String,
    mobileText: String,
    wifiText: String,
    totalText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mobile:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)))
                Text(mobileText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("WiFi:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)))
                Text(wifiText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(6.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = accentColor))
                Text(totalText, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, color = accentColor))
            }
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 2.dp),
        style = if (isHeader) {
            MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                color = color,
                fontSize = 13.sp
            )
        } else {
            MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = color,
                fontSize = 12.sp
            )
        },
        textAlign = TextAlign.Center
    )
}
