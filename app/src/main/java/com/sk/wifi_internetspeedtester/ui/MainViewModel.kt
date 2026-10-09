package com.sk.wifi_internetspeedtester.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sk.wifi_internetspeedtester.data.repository.DataUsageRepository
import com.sk.wifi_internetspeedtester.data.repository.NetworkRepository
import com.sk.wifi_internetspeedtester.data.repository.RealSpeedTestRepository
import com.sk.wifi_internetspeedtester.data.repository.SpeedTestRepository
import com.sk.wifi_internetspeedtester.domain.interpreter.SmartResultInterpreter
import com.sk.wifi_internetspeedtester.domain.model.DailyDataUsage
import com.sk.wifi_internetspeedtester.domain.model.MonthlyUsageSummary
import com.sk.wifi_internetspeedtester.domain.model.NetworkStatus
import com.sk.wifi_internetspeedtester.domain.model.SmartExplanation
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestResult
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestStage
import com.sk.wifi_internetspeedtester.domain.model.SpeedTestState
import com.sk.wifi_internetspeedtester.service.LiveTrafficData
import com.sk.wifi_internetspeedtester.service.SpeedMeterService
import com.sk.wifi_internetspeedtester.service.TrafficStatsMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainTab {
    LIVE_METER,
    DATA_USAGE,
    SPEED_TEST
}

enum class Screen {
    DASHBOARD,
    TESTING,
    RESULT
}

data class MainUiState(
    val currentTab: MainTab = MainTab.LIVE_METER,
    val liveTraffic: LiveTrafficData = LiveTrafficData(),
    val speedHistory: List<Long> = List(20) { 0L },
    val dailyDataHistory: List<DailyDataUsage> = emptyList(),
    val monthlyDataSummary: MonthlyUsageSummary = MonthlyUsageSummary(mobileBytes = 0L, wifiBytes = 0L),
    val isNotificationServiceEnabled: Boolean = false,
    val isStartOnBootEnabled: Boolean = true,
    val isSettingsOpen: Boolean = false,
    val networkStatus: NetworkStatus = NetworkStatus(),
    val speedTestState: SpeedTestState = SpeedTestState(),
    val lastResult: SpeedTestResult? = null,
    val smartExplanation: SmartExplanation? = null,
    val hasLocationPermission: Boolean = false,
    val currentScreen: Screen = Screen.DASHBOARD
)

class MainViewModel(
    application: Application,
    private val networkRepository: NetworkRepository = NetworkRepository(application),
    private val speedTestRepository: SpeedTestRepository = RealSpeedTestRepository(application),
    private val dataUsageRepository: DataUsageRepository = DataUsageRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val settingsPrefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    init {
        val startOnBoot = settingsPrefs.getBoolean("start_on_boot", true)
        val notifEnabled = settingsPrefs.getBoolean("notification_service_enabled", false)

        _uiState.update {
            it.copy(
                isStartOnBootEnabled = startOnBoot,
                isNotificationServiceEnabled = notifEnabled
            )
        }

        // Observe real-time network connectivity
        viewModelScope.launch {
            networkRepository.getNetworkStatusFlow().collectLatest { status ->
                _uiState.update { it.copy(networkStatus = status) }
            }
        }

        // Real-time Traffic Sampling loop & Usage Logging
        viewModelScope.launch {
            while (isActive) {
                val data = TrafficStatsMonitor.sampleLiveTraffic()
                val netStatus = _uiState.value.networkStatus

                val transferredBytes = data.downloadBytesPerSec + data.uploadBytesPerSec
                if (transferredBytes > 0) {
                    dataUsageRepository.recordUsage(netStatus.type, transferredBytes)
                }

                val history = dataUsageRepository.getDailyHistory()
                val monthly = dataUsageRepository.getMonthlySummary()

                _uiState.update { current ->
                    val newHistory = (current.speedHistory.drop(1) + data.downloadBytesPerSec)
                    current.copy(
                        liveTraffic = data,
                        speedHistory = newHistory,
                        dailyDataHistory = history,
                        monthlyDataSummary = monthly,
                        isNotificationServiceEnabled = SpeedMeterService.isRunning
                    )
                }
                delay(1000)
            }
        }

        // Observe speed test state
        viewModelScope.launch {
            speedTestRepository.speedTestState.collectLatest { testState ->
                _uiState.update { current ->
                    val screen = when (testState.stage) {
                        SpeedTestStage.CONNECTING,
                        SpeedTestStage.PING,
                        SpeedTestStage.DOWNLOAD,
                        SpeedTestStage.UPLOAD,
                        SpeedTestStage.FINALIZING -> Screen.TESTING
                        SpeedTestStage.COMPLETED -> Screen.RESULT
                        SpeedTestStage.IDLE,
                        SpeedTestStage.ERROR,
                        SpeedTestStage.CANCELLED -> if (current.lastResult != null && testState.stage == SpeedTestStage.IDLE) current.currentScreen else Screen.DASHBOARD
                    }
                    current.copy(
                        speedTestState = testState,
                        currentScreen = screen
                    )
                }
            }
        }

        // Observe last result & generate AI smart interpretation
        viewModelScope.launch {
            speedTestRepository.lastResult.collectLatest { result ->
                val explanation = result?.let { SmartResultInterpreter.interpret(it) }
                _uiState.update {
                    it.copy(
                        lastResult = result,
                        smartExplanation = explanation
                    )
                }
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(currentTab = tab, isSettingsOpen = false) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun toggleNotificationService(enable: Boolean) {
        val app = getApplication<Application>()
        settingsPrefs.edit().putBoolean("notification_service_enabled", enable).apply()
        if (enable) {
            SpeedMeterService.start(app)
        } else {
            SpeedMeterService.stop(app)
        }
        _uiState.update { it.copy(isNotificationServiceEnabled = enable) }
    }

    fun toggleStartOnBoot(enable: Boolean) {
        settingsPrefs.edit().putBoolean("start_on_boot", enable).apply()
        _uiState.update { it.copy(isStartOnBootEnabled = enable) }
    }

    fun startSpeedTest() {
        val status = _uiState.value.networkStatus
        _uiState.update {
            it.copy(
                currentTab = MainTab.SPEED_TEST,
                currentScreen = Screen.TESTING,
                isSettingsOpen = false
            )
        }
        speedTestRepository.startTest(status)
    }

    fun cancelTest() {
        speedTestRepository.cancelTest()
        _uiState.update { it.copy(currentScreen = Screen.DASHBOARD) }
    }

    fun navigateToDashboard() {
        speedTestRepository.cancelTest()
        _uiState.update { it.copy(currentScreen = Screen.DASHBOARD) }
    }

    fun updatePermissionState(hasPermission: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
        val currentStatus = networkRepository.getCurrentNetworkStatus()
        _uiState.update { it.copy(networkStatus = currentStatus) }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(application) as T
        }
    }
}
