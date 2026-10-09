package com.sk.wifi_internetspeedtester

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sk.wifi_internetspeedtester.domain.interpreter.SmartResultInterpreter
import com.sk.wifi_internetspeedtester.domain.model.NetworkType
import com.sk.wifi_internetspeedtester.domain.model.SmartExplanation
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestResult
import com.sk.wifi_internetspeedtester.ui.MainTab
import com.sk.wifi_internetspeedtester.ui.MainUiState
import com.sk.wifi_internetspeedtester.ui.MainViewModel
import com.sk.wifi_internetspeedtester.ui.Screen
import com.sk.wifi_internetspeedtester.ui.screens.DashboardScreen
import com.sk.wifi_internetspeedtester.ui.screens.DataUsageScreen
import com.sk.wifi_internetspeedtester.ui.screens.LiveMeterScreen
import com.sk.wifi_internetspeedtester.ui.screens.ResultScreen
import com.sk.wifi_internetspeedtester.ui.screens.SettingsScreen
import com.sk.wifi_internetspeedtester.ui.screens.TestingScreen
import com.sk.wifi_internetspeedtester.ui.theme.WiFiInternetSpeedTesterTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var onPermissionResultCallback: ((Boolean) -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onPermissionResultCallback?.invoke(granted)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WiFiInternetSpeedTesterTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(application)
                )
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    val hasFine = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    val hasCoarse = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    viewModel.updatePermissionState(hasFine || hasCoarse)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (uiState.currentScreen != Screen.TESTING && !uiState.isSettingsOpen) {
                            BottomNavigationBar(
                                currentTab = uiState.currentTab,
                                onSelectTab = { viewModel.selectTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    MainScreenContent(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onStartTest = { viewModel.startSpeedTest() },
                        onCancelTest = { viewModel.cancelTest() },
                        onBackToDashboard = { viewModel.navigateToDashboard() },
                        onOpenSettings = { viewModel.openSettings() },
                        onCloseSettings = { viewModel.closeSettings() },
                        onToggleStartOnBoot = { viewModel.toggleStartOnBoot(it) },
                        onRequestPermission = {
                            onPermissionResultCallback = { granted ->
                                viewModel.updatePermissionState(granted)
                            }
                            val perms = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        },
                        onToggleNotificationService = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            viewModel.toggleNotificationService(enable)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            selected = currentTab == MainTab.LIVE_METER,
            onClick = { onSelectTab(MainTab.LIVE_METER) },
            icon = { Icon(imageVector = Icons.Default.Speed, contentDescription = null) },
            label = { Text("Live Meter") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )

        NavigationBarItem(
            selected = currentTab == MainTab.DATA_USAGE,
            onClick = { onSelectTab(MainTab.DATA_USAGE) },
            icon = { Icon(imageVector = Icons.Default.Assessment, contentDescription = null) },
            label = { Text("Data Log") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )

        NavigationBarItem(
            selected = currentTab == MainTab.SPEED_TEST,
            onClick = { onSelectTab(MainTab.SPEED_TEST) },
            icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) },
            label = { Text("Speed Test") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun MainScreenContent(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onStartTest: () -> Unit,
    onCancelTest: () -> Unit,
    onBackToDashboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onToggleStartOnBoot: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onToggleNotificationService: (Boolean) -> Unit
) {
    val context = LocalContext.current

    if (uiState.isSettingsOpen) {
        SettingsScreen(
            isNotificationServiceEnabled = uiState.isNotificationServiceEnabled,
            isStartOnBootEnabled = uiState.isStartOnBootEnabled,
            onToggleNotificationService = onToggleNotificationService,
            onToggleStartOnBoot = onToggleStartOnBoot,
            onBack = onCloseSettings
        )
    } else {
        Crossfade(
            targetState = Pair(uiState.currentTab, uiState.currentScreen),
            modifier = modifier.fillMaxSize(),
            label = "screenTransition"
        ) { (tab, screen) ->
            when {
                tab == MainTab.LIVE_METER -> {
                    LiveMeterScreen(
                        liveTraffic = uiState.liveTraffic,
                        speedHistory = uiState.speedHistory,
                        networkStatus = uiState.networkStatus,
                        isNotificationServiceEnabled = uiState.isNotificationServiceEnabled,
                        onToggleNotificationService = onToggleNotificationService,
                        onOpenSettings = onOpenSettings
                    )
                }
                tab == MainTab.DATA_USAGE -> {
                    DataUsageScreen(
                        dailyHistory = uiState.dailyDataHistory,
                        monthlySummary = uiState.monthlyDataSummary
                    )
                }
                tab == MainTab.SPEED_TEST -> {
                    when (screen) {
                        Screen.DASHBOARD -> {
                            DashboardScreen(
                                networkStatus = uiState.networkStatus,
                                lastResult = uiState.lastResult,
                                hasLocationPermission = uiState.hasLocationPermission,
                                onStartTest = onStartTest,
                                onRequestPermission = onRequestPermission,
                                onViewLastResult = {},
                                onOpenSettings = onOpenSettings
                            )
                        }
                        Screen.TESTING -> {
                            TestingScreen(
                                state = uiState.speedTestState,
                                onCancelTest = onCancelTest
                            )
                        }
                        Screen.RESULT -> {
                            val result = uiState.lastResult
                            if (result != null) {
                                ResultScreen(
                                    result = result,
                                    explanation = uiState.smartExplanation,
                                    onTestAgain = onStartTest,
                                    onBackToDashboard = onBackToDashboard,
                                    onShareResult = {
                                        shareSpeedTestResult(context, result, uiState.smartExplanation)
                                    }
                                )
                            } else {
                                DashboardScreen(
                                    networkStatus = uiState.networkStatus,
                                    lastResult = null,
                                    hasLocationPermission = uiState.hasLocationPermission,
                                    onStartTest = onStartTest,
                                    onRequestPermission = onRequestPermission,
                                    onViewLastResult = {},
                                    onOpenSettings = onOpenSettings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareSpeedTestResult(
    context: Context,
    result: SpeedTestResult,
    explanation: SmartExplanation?
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(result.timestamp))
    val dl = SmartResultInterpreter.formatSpeed(result.downloadMbps)
    val ul = SmartResultInterpreter.formatSpeed(result.uploadMbps)
    val ping = SmartResultInterpreter.formatPing(result.pingMs)
    val jitter = SmartResultInterpreter.formatPing(result.jitterMs)
    val packetLoss = String.format(Locale.US, "%.1f", result.packetLossPercent)
    val isp = result.ispName ?: "Not Specified"
    val location = result.locationName ?: "Location unavailable"
    val wifiSSID = if (result.networkType == NetworkType.WIFI) result.networkName else "N/A"

    val text = """
        🚀 SpeedTester Results
        -----------------------
        Download: $dl Mbps
        Upload: $ul Mbps
        Ping: $ping ms | Jitter: $jitter ms
        Packet Loss: $packetLoss% | Stability: ${result.stabilityPercent.toInt()}%
        
        Quality: ${result.overallQuality.name}
        Network: ${result.networkTypeLabel} ($wifiSSID)
        ISP/Carrier: $isp
        Location: $location
        Server: Automatically Selected
        Date: $dateStr
        
        Interpretation: ${explanation?.headline ?: "Test Complete"}
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "My Internet Speed Test Result")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Speed Test Results"))
}
