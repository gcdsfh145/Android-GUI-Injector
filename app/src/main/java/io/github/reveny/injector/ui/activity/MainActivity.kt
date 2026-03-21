package io.github.reveny.injector.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.reveny.injector.App
import io.github.reveny.injector.BuildConfig
import io.github.reveny.injector.R
import io.github.reveny.injector.core.InjectorData
import io.github.reveny.injector.core.LogManager
import io.github.reveny.injector.core.Utility
import io.github.reveny.injector.core.root.RootHandler
import io.github.reveny.injector.core.root.RootManager
import io.github.reveny.injector.util.ThemeUtil
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainActivity : BaseActivity() {

    private val viewModel by viewModels<MainViewModel> {
        MainViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InjectorComposeTheme {
                MainRoute(viewModel = viewModel)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
    }
}

private class MainViewModel(private val appContext: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        refreshOverview()
        refreshApps()
        refreshLogs()
    }

    fun refreshOverview() {
        viewModelScope.launch(Dispatchers.IO) {
            val packageNames = arrayOf(
                "io.github.huskydg.magisk",
                "me.weishu.kernelsu",
                "me.bmax.apatch",
                "io.github.vvb2060.magisk",
                "com.topjohnwu.magisk",
                "com.sukisu.ultra"
            )
            _uiState.update {
                it.copy(
                    overview = DeviceOverview(
                        systemVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        device = "${Build.BRAND} ${Build.MODEL}",
                        primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
                        isRooted = Utility.isRooted(),
                        rootSystem = checkRootSolution(packageNames),
                        zygiskStatus = checkZygiskStatusWithRoot(),
                        isEmulator = Utility.isEmulator(),
                        securityStatus = checkSecurityStatusWithRoot()
                    )
                )
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val packageManager = appContext.packageManager
            val packages = packageManager.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != appContext.packageName }
                .sortedBy { it.packageName.lowercase() }
                .map {
                    AppEntry(
                        label = it.loadLabel(packageManager).toString(),
                        packageName = it.packageName,
                        uid = it.uid,
                        launcherActivity = Utility.getLaunchActivity(appContext, it.packageName)
                    )
                }
            _uiState.update { it.copy(installedApps = packages) }
        }
    }

    fun refreshLogs() {
        _uiState.update { it.copy(logs = LogManager.GetLogs().toList()) }
    }

    fun switchTab(tab: MainTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == MainTab.LOGS) {
            refreshLogs()
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectApp(app: AppEntry) {
        _uiState.update {
            val updated = it.injection.copy(
                selectedAppLabel = "${app.label} (${app.packageName})",
                packageName = app.packageName,
                appUid = app.uid,
                launcherActivity = app.launcherActivity,
                processId = RootManager.instance?.getPid(app.packageName) ?: "-1"
            )
            LogManager.AddLog("Selected: ${app.label} (${app.packageName})")
            it.copy(injection = updated, logs = LogManager.GetLogs().toList())
        }
    }

    fun updateLibraryPath(path: String) {
        _uiState.update {
            val injectType = if (path.endsWith(".dex")) 1 else it.injection.injectType
            it.copy(
                injection = it.injection.copy(
                    libraryPath = path,
                    injectType = injectType
                )
            )
        }
    }

    fun updateInjectType(type: Int) {
        _uiState.update { it.copy(injection = it.injection.copy(injectType = type)) }
    }

    fun updateDexClassName(value: String) {
        _uiState.update { it.copy(injection = it.injection.copy(dexClassName = value)) }
    }

    fun updateDexMethodName(value: String) {
        _uiState.update { it.copy(injection = it.injection.copy(dexMethodName = value)) }
    }

    fun updateAutoLaunch(value: Boolean) {
        _uiState.update { it.copy(injection = it.injection.copy(shouldAutoLaunch = value)) }
        LogManager.AddLog("Auto Launch: $value")
        refreshLogs()
    }

    fun updateKillBeforeLaunch(value: Boolean) {
        _uiState.update { it.copy(injection = it.injection.copy(shouldKillBeforeLaunch = value)) }
        LogManager.AddLog("Kill Process: $value")
        refreshLogs()
    }

    fun updateRemap(value: Boolean) {
        _uiState.update { it.copy(injection = it.injection.copy(remapLibrary = value)) }
        LogManager.AddLog("Remap Library: $value")
        refreshLogs()
    }

    fun updateUseProxy(value: Boolean) {
        _uiState.update {
            val current = it.injection
            it.copy(injection = current.copy(useProxy = value))
        }
        LogManager.AddLog("Use Proxy: $value")
        refreshLogs()
    }

    fun updateRandomizeProxy(value: Boolean) {
        _uiState.update {
            val current = it.injection
            it.copy(
                injection = current.copy(
                    useProxy = current.useProxy || value,
                    randomizeProxyName = value
                )
            )
        }
        LogManager.AddLog("Randomize Proxy: $value")
        refreshLogs()
    }

    fun updateHideLibrary(value: Boolean) {
        _uiState.update {
            val current = it.injection
            it.copy(
                injection = current.copy(
                    useProxy = current.useProxy || value,
                    hideLibrary = value
                )
            )
        }
        LogManager.AddLog("Hide Library: $value")
        refreshLogs()
    }

    fun updateBypassRestrictions(value: Boolean) {
        if (value && Utility.isEmulator()) {
            LogManager.AddLog("Emulator detected, bypass restrictions disabled")
            refreshLogs()
            return
        }
        _uiState.update { it.copy(injection = it.injection.copy(bypassNamespaceRestrictions = value)) }
        LogManager.AddLog("Bypass Restrictions: $value")
        refreshLogs()
    }

    fun startInjection(activity: Activity, onResult: (String) -> Unit) {
        val state = uiState.value.injection
        if (state.packageName.isBlank()) {
            onResult("Please select a target package")
            return
        }
        if (state.libraryPath.isBlank()) {
            onResult("Please select a library path")
            return
        }
        if (RootManager.instance?.hasRootAccess != true) {
            onResult("Root access not granted, please restart the app")
            return
        }

        val data = InjectorData().apply {
            setPackageName(state.packageName)
            setLauncherActivity(state.launcherActivity)
            setLibraryPath(state.libraryPath)
            setInjectType(state.injectType)
            setAppUid(state.appUid)
            setDexClassName(state.dexClassName)
            setDexMethodName(state.dexMethodName)
            setShouldAutoLaunch(state.shouldAutoLaunch)
            setShouldKillBeforeLaunch(state.shouldKillBeforeLaunch)
            setRemapLibrary(state.remapLibrary)
            setUseProxy(state.useProxy)
            setRandomizeProxyName(state.randomizeProxyName)
            setHideLibrary(state.hideLibrary)
            setBypassNamespaceRestrictions(state.bypassNamespaceRestrictions)
        }

        LogManager.AddLog("Starting Injection...")
        LogManager.AddLog("Injection Data: $data")
        refreshLogs()

        try {
            RootHandler().Inject(activity)
            onResult("Injection requested")
        } catch (_: Throwable) {
            onResult("Failed to start injection")
        }
    }

    fun copyLogsText(): String = uiState.value.logs.joinToString(separator = "\n")

    fun clearLogs() {
        LogManager.logs.clear()
        refreshLogs()
    }

    fun updateSettings(block: (SettingsUiState) -> SettingsUiState) {
        _uiState.update { it.copy(settings = block(it.settings)) }
    }

    private fun checkRootSolution(packageNames: Array<String>): String {
        val packageManager = appContext.packageManager
        for (packageName in packageNames) {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_ACTIVITIES)
                val appInfo = packageInfo.applicationInfo ?: continue
                return packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
            }
        }

        val magiskPaths = arrayOf(
            "/sbin/magisk",
            "/system/bin/magisk",
            "/system/xbin/magisk",
            "/data/adb/magisk",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su"
        )

        return if (magiskPaths.any { File(it).exists() }) "Magisk" else "Not detected"
    }

    private fun checkZygiskStatusWithRoot(): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c ls /data/adb/modules")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = buildString {
                reader.forEachLine { append(it).append('\n') }
            }
            process.waitFor()
            if (output.contains("zygisksu") || output.contains("zygisk")) "Zygisk Detected" else "Not detected"
        } catch (_: Exception) {
            "Not detected"
        }
    }

    private fun checkSecurityStatusWithRoot(): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c getenforce")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val status = reader.readLine()?.trim().orEmpty()
            process.waitFor()
            if (status.isNotEmpty()) "SELinux: $status" else "SELinux: Unknown"
        } catch (_: Exception) {
            "SELinux: Unknown"
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(context.applicationContext) as T
            }
        }
    }
}

private data class MainUiState(
    val currentTab: MainTab = MainTab.HOME,
    val overview: DeviceOverview = DeviceOverview(),
    val installedApps: List<AppEntry> = emptyList(),
    val searchQuery: String = "",
    val injection: InjectionUiState = InjectionUiState(),
    val logs: List<String> = emptyList(),
    val settings: SettingsUiState = SettingsUiState()
)

private data class DeviceOverview(
    val systemVersion: String = "Loading...",
    val device: String = "Loading...",
    val primaryAbi: String = "Loading...",
    val isRooted: Boolean = false,
    val rootSystem: String = "Checking...",
    val zygiskStatus: String = "Checking...",
    val isEmulator: Boolean = false,
    val securityStatus: String = "Checking..."
)

private data class AppEntry(
    val label: String,
    val packageName: String,
    val uid: Int,
    val launcherActivity: String
)

private data class InjectionUiState(
    val selectedAppLabel: String = "",
    val packageName: String = "",
    val appUid: Int = -1,
    val launcherActivity: String = "",
    val processId: String = "-1",
    val libraryPath: String = "",
    val injectType: Int = 0,
    val dexClassName: String = "io.github.reveny.dex.Main",
    val dexMethodName: String = "main",
    val shouldAutoLaunch: Boolean = false,
    val shouldKillBeforeLaunch: Boolean = false,
    val remapLibrary: Boolean = false,
    val useProxy: Boolean = false,
    val randomizeProxyName: Boolean = false,
    val hideLibrary: Boolean = false,
    val bypassNamespaceRestrictions: Boolean = false
)

private data class SettingsUiState(
    val followSystemAccent: Boolean = App.getPreferences().getBoolean("follow_system_accent", true),
    val blackTheme: Boolean = App.getPreferences().getBoolean("black_dark_theme", false),
    val darkTheme: String = App.getPreferences().getString("dark_theme", ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM) ?: ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM,
    val language: String = App.getPreferences().getString("language", "SYSTEM") ?: "SYSTEM",
    val themeColor: String = App.getPreferences().getString("theme_color", "COLOR_BLUE") ?: "COLOR_BLUE"
)

private data class LanguageOption(
    val tag: String,
    val labelRes: Int,
    val noteRes: Int
)

private val languageOptions = listOf(
    LanguageOption("SYSTEM", R.string.language_system, R.string.language_system_summary),
    LanguageOption("zh-CN", R.string.language_simplified_chinese, R.string.language_simplified_chinese_summary),
    LanguageOption("en", R.string.language_english, R.string.language_english_summary)
)

private fun Context.localizedContext(tag: String): Context {
    if (tag == "SYSTEM") return this
    val locale = App.getLocale(tag)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

private fun localizedText(context: Context, tag: String, resId: Int): String {
    return context.localizedContext(tag).resources.getString(resId)
}

private fun languageDisplayName(context: Context, tag: String): String {
    val option = languageOptions.firstOrNull { it.tag == tag } ?: languageOptions.first()
    return localizedText(context, tag, option.labelRes)
}

private enum class MainTab(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_overview, Icons.Outlined.Info),
    INJECTION(R.string.tab_injection, Icons.Outlined.Bolt),
    LOGS(R.string.tab_logs, Icons.AutoMirrored.Outlined.Article),
    SETTINGS(R.string.tab_settings, Icons.Outlined.Settings)
}

@Composable
private fun tabLabel(tab: MainTab): String = stringResource(tab.labelRes)

@Composable
private fun MainRoute(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val path = uri?.path
            ?.replace(
                "/document/primary:",
                Environment.getExternalStorageDirectory().path + "/"
            )
            .orEmpty()

        if (path.endsWith(".so") || path.endsWith(".dex")) {
            viewModel.updateLibraryPath(path)
        } else if (uri != null) {
            scope.launch { snackbars.showSnackbar("Invalid file type selected. Please select a .so or .dex file.") }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF2EEE5), Color(0xFFF8F5EF), Color(0xFFEDE5D7))
                )
            )
            .safeDrawingPadding(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            MainTopBar(
                currentTab = uiState.currentTab,
                onRefresh = {
                    viewModel.refreshOverview()
                    viewModel.refreshApps()
                    viewModel.refreshLogs()
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = uiState.currentTab == tab,
                        onClick = { viewModel.switchTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = tabLabel(tab)) },
                        label = { Text(tabLabel(tab)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(240)) togetherWith fadeOut(animationSpec = tween(180))
            },
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                MainTab.HOME -> HomeScreen(uiState.overview)
                MainTab.INJECTION -> InjectionScreen(
                    state = uiState,
                    onSearchChange = viewModel::updateSearch,
                    onSelectApp = viewModel::selectApp,
                    onPickLibrary = { picker.launch("*/*") },
                    onInjectTypeChange = viewModel::updateInjectType,
                    onDexClassChange = viewModel::updateDexClassName,
                    onDexMethodChange = viewModel::updateDexMethodName,
                    onAutoLaunchChange = viewModel::updateAutoLaunch,
                    onKillBeforeLaunchChange = viewModel::updateKillBeforeLaunch,
                    onRemapChange = viewModel::updateRemap,
                    onUseProxyChange = viewModel::updateUseProxy,
                    onRandomizeProxyChange = viewModel::updateRandomizeProxy,
                    onHideLibraryChange = viewModel::updateHideLibrary,
                    onBypassChange = viewModel::updateBypassRestrictions,
                    onStartInjection = {
                        viewModel.startInjection(context as Activity) { message ->
                            scope.launch { snackbars.showSnackbar(message) }
                        }
                    }
                )
                MainTab.LOGS -> LogsScreen(
                    logs = uiState.logs,
                    onCopy = {
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            android.content.ClipData.newPlainText("logs", viewModel.copyLogsText())
                        )
                        scope.launch { snackbars.showSnackbar("Logs copied") }
                    },
                    onClear = viewModel::clearLogs
                )
                MainTab.SETTINGS -> SettingsScreen(
                    state = uiState.settings,
                    onUpdate = viewModel::updateSettings,
                    onApply = { message -> scope.launch { snackbars.showSnackbar(message) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(currentTab: MainTab, onRefresh: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GUI Injector", fontWeight = FontWeight.SemiBold)
                Text(
                    tabLabel(currentTab),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        ),
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = "Refresh")
            }
        }
    )
}

@Composable
private fun HomeScreen(overview: DeviceOverview) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF29323C), Color(0xFF485563), Color(0xFFD3CBB8))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Android GUI Injector", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "现代化 Compose 外壳，保留原有 root 与 native 注入能力。",
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill("${BuildConfig.VERSION_NAME}", Icons.Outlined.Android)
                            StatusPill(if (overview.isRooted) "Rooted" else "No Root", Icons.Outlined.Shield)
                            StatusPill(overview.primaryAbi, Icons.Outlined.Memory)
                        }
                    }
                }
            }
        }

        item {
            InfoCard(
                title = "System Snapshot",
                items = listOf(
                    "System" to overview.systemVersion,
                    "Device" to overview.device,
                    "ABI" to overview.primaryAbi,
                    "Root" to if (overview.isRooted) "Yes" else "No",
                    "Root Solution" to overview.rootSystem,
                    "Zygisk" to overview.zygiskStatus,
                    "Emulator" to if (overview.isEmulator) "Yes" else "No",
                    "Security" to overview.securityStatus
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoCard(title: String, items: List<Pair<String, String>>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { (label, value) ->
                    OutlinedCard(modifier = Modifier.width(160.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, icon: ImageVector) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun InjectionScreen(
    state: MainUiState,
    onSearchChange: (String) -> Unit,
    onSelectApp: (AppEntry) -> Unit,
    onPickLibrary: () -> Unit,
    onInjectTypeChange: (Int) -> Unit,
    onDexClassChange: (String) -> Unit,
    onDexMethodChange: (String) -> Unit,
    onAutoLaunchChange: (Boolean) -> Unit,
    onKillBeforeLaunchChange: (Boolean) -> Unit,
    onRemapChange: (Boolean) -> Unit,
    onUseProxyChange: (Boolean) -> Unit,
    onRandomizeProxyChange: (Boolean) -> Unit,
    onHideLibraryChange: (Boolean) -> Unit,
    onBypassChange: (Boolean) -> Unit,
    onStartInjection: () -> Unit
) {
    var appSheetOpen by rememberSaveable { mutableStateOf(false) }
    val filteredApps = remember(state.installedApps, state.searchQuery) {
        if (state.searchQuery.isBlank()) state.installedApps
        else state.installedApps.filter {
            val target = "${it.label} (${it.packageName})".lowercase()
            target.contains(state.searchQuery.lowercase())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(28.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1F4037), Color(0xFF99F2C8))
                                )
                            )
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Injection Console", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(
                            "选择目标应用、装载文件与注入参数，使用新的 Compose 表单替代原先的 Fragment 页面。",
                            color = Color.White.copy(alpha = 0.84f)
                        )
                    }
                }
            }

            item {
                SectionCard("Target") {
                    ReadonlyField(
                        label = "Target App",
                        value = state.injection.selectedAppLabel.ifBlank { "Tap to choose an installed app" },
                        buttonLabel = "Browse",
                        onClick = { appSheetOpen = true }
                    )
                    Spacer(Modifier.height(12.dp))
                    StatusRow(
                        "Package" to state.injection.packageName.ifBlank { "Not selected" },
                        "PID" to state.injection.processId,
                        "UID" to if (state.injection.appUid >= 0) state.injection.appUid.toString() else "-"
                    )
                }
            }

            item {
                SectionCard("Payload") {
                    TabRow(selectedTabIndex = state.injection.injectType) {
                        listOf("Shared Library", "DEX Entry").forEachIndexed { index, title ->
                            Tab(
                                selected = state.injection.injectType == index,
                                onClick = { onInjectTypeChange(index) },
                                text = { Text(title) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ReadonlyField(
                        label = if (state.injection.injectType == 0) "Library Path (.so)" else "DEX Path (.dex)",
                        value = state.injection.libraryPath.ifBlank { "Choose a payload file" },
                        buttonLabel = "Pick File",
                        onClick = onPickLibrary
                    )
                    AnimatedVisibility(visible = state.injection.injectType == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = state.injection.dexClassName,
                                onValueChange = onDexClassChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("DEX Class Name") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.injection.dexMethodName,
                                onValueChange = onDexMethodChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("DEX Method Name") },
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item {
                SectionCard("Behavior") {
                    ToggleRow("Auto Launch", state.injection.shouldAutoLaunch, onAutoLaunchChange)
                    ToggleRow("Kill Before Auto Launch", state.injection.shouldKillBeforeLaunch, onKillBeforeLaunchChange)
                    AnimatedVisibility(visible = state.injection.injectType == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Spacer(Modifier.height(4.dp))
                            ToggleRow("Remap Library", state.injection.remapLibrary, onRemapChange)
                            ToggleRow("Use Proxy Library", state.injection.useProxy, onUseProxyChange)
                            ToggleRow("Randomize Proxy Library", state.injection.randomizeProxyName, onRandomizeProxyChange)
                            ToggleRow("Hide Injected Library", state.injection.hideLibrary, onHideLibraryChange)
                            ToggleRow("Bypass Namespace Restrictions", state.injection.bypassNamespaceRestrictions, onBypassChange)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onStartInjection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Outlined.Terminal, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Start Injection")
                }
            }
        }

        if (appSheetOpen) {
            AppPickerSheet(
                query = state.searchQuery,
                apps = filteredApps,
                onQueryChange = onSearchChange,
                onDismiss = { appSheetOpen = false },
                onPick = {
                    onSelectApp(it)
                    appSheetOpen = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    query: String,
    apps: List<AppEntry>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPick: (AppEntry) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Select Target App", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search apps") },
                singleLine = true
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(apps) { app ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(app) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Apps, contentDescription = null)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        Box(modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReadonlyField(label: String, value: String, buttonLabel: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                Text(value, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = onClick) {
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(vararg entries: Pair<String, String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        entries.forEach { (label, value) ->
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LogsScreen(logs: List<String>, onCopy: () -> Unit, onClear: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF232526), Color(0xFF414345))))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Runtime Logs", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("读取 LogManager 中的运行日志，方便查看注入流程与 native 返回信息。", color = Color.White.copy(alpha = 0.78f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AssistChip(onClick = onCopy, label = { Text("Copy") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) })
                        AssistChip(onClick = onClear, label = { Text("Clear") }, leadingIcon = { Icon(Icons.Outlined.WarningAmber, null) })
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No logs yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("触发一次 root 检查或注入后，这里会显示新的日志内容。")
                    }
                }
            }
        } else {
            items(logs.reversed()) { log ->
                OutlinedCard(shape = RoundedCornerShape(18.dp)) {
                    Text(log, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onUpdate: ((SettingsUiState) -> SettingsUiState) -> Unit,
    onApply: (String) -> Unit
) {
    val preferences = remember { App.getPreferences() }
    val context = LocalContext.current
    val activity = context as Activity
    val darkThemeOptions = listOf(
        "Light" to ThemeUtil.MODE_NIGHT_NO,
        "Dark" to ThemeUtil.MODE_NIGHT_YES,
        "System" to ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM
    )
    val colorOptions = listOf(
        "Sakura" to "SAKURA",
        "Blue" to "MATERIAL_BLUE",
        "Teal" to "MATERIAL_TEAL",
        "Green" to "MATERIAL_GREEN",
        "Amber" to "MATERIAL_AMBER"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF4B6CB7), Color(0xFF182848))))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("先用 Compose 重建常用主题与语言选项，保持与现有 SharedPreferences 键兼容。", color = Color.White.copy(alpha = 0.82f))
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.settings_group_system)) {
                Text(
                    stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    stringResource(R.string.settings_language_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languageOptions.forEach { option ->
                        FilterChip(
                            selected = state.language == option.tag,
                            onClick = {
                                preferences.edit().putString("language", option.tag).apply()
                                App.setLocaleTag(option.tag)
                                onUpdate { it.copy(language = option.tag) }
                                activity.recreate()
                            },
                            label = { Text(localizedText(context, option.tag, option.labelRes)) }
                        )
                    }
                }
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.language_current),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            languageDisplayName(context, state.language),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            localizedText(
                                context,
                                state.language,
                                languageOptions.firstOrNull { it.tag == state.language }?.noteRes
                                    ?: R.string.language_system_summary
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SectionCard("Theme") {
                ToggleRow("Follow system accent", state.followSystemAccent) { value ->
                    preferences.edit().putBoolean("follow_system_accent", value).apply()
                    onUpdate { it.copy(followSystemAccent = value) }
                    onApply("Accent preference updated. Restart app to fully apply.")
                }
                ToggleRow("Pure black dark theme", state.blackTheme) { value ->
                    preferences.edit().putBoolean("black_dark_theme", value).apply()
                    onUpdate { it.copy(blackTheme = value) }
                    onApply("Black theme preference updated. Restart app to fully apply.")
                }
                Text("Dark mode", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    darkThemeOptions.forEach { (label, value) ->
                        FilterChip(
                            selected = state.darkTheme == value,
                            onClick = {
                                preferences.edit().putString("dark_theme", value).apply()
                                onUpdate { it.copy(darkTheme = value) }
                                onApply("Dark theme preference updated. Restart app to fully apply.")
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Accent", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { (label, value) ->
                        FilterChip(
                            selected = state.themeColor == value,
                            onClick = {
                                preferences.edit().putString("theme_color", value).apply()
                                onUpdate { it.copy(themeColor = value) }
                                onApply("Theme color updated. Restart app to fully apply.")
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InjectorComposeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val useDark = when (ThemeUtil.getDarkTheme()) {
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> true
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> isSystemInDarkTheme()
    }

    val light = lightColorScheme(
        primary = Color(0xFF295F4E),
        secondary = Color(0xFF9F6F3A),
        tertiary = Color(0xFF5F7C8D),
        background = Color(0xFFF7F1E8),
        surface = Color(0xFFFFFBF5)
    )
    val dark = darkColorScheme(
        primary = Color(0xFF82D8B4),
        secondary = Color(0xFFF0BE7A),
        tertiary = Color(0xFFA7C6DA),
        background = Color(0xFF111715),
        surface = Color(0xFF19211E)
    )

    val colorScheme = when {
        ThemeUtil.isSystemAccent() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> dark
        else -> light
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
