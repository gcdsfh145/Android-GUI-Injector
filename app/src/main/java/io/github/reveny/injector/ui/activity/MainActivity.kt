package io.github.reveny.injector.ui.activity

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    private val viewModel by viewModels<MainViewModel> {
        MainViewModel.factory(applicationContext)
    }
    private val rootHandler = RootHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InjectorComposeTheme {
                MainRoute(
                    viewModel = viewModel,
                    rootHandler = rootHandler
                )
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
        refreshAll()
    }

    fun refreshAll() {
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
            val rootGranted = RootManager.instance?.hasRootAccess == true
            _uiState.update {
                it.copy(
                    overview = DeviceOverview(
                        systemVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        device = "${Build.BRAND} ${Build.MODEL}",
                        primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
                        isRooted = Utility.isRooted(),
                        rootGranted = rootGranted,
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
            val packages = packageManager
                .getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                .filter {
                    (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 &&
                        it.packageName != appContext.packageName
                }
                .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
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
            it.copy(
                injection = it.injection.copy(
                    selectedAppLabel = app.label,
                    packageName = app.packageName,
                    appUid = app.uid,
                    launcherActivity = app.launcherActivity,
                    processId = RootManager.instance?.getPid(app.packageName) ?: "-1"
                )
            )
        }
        LogManager.AddLog("Selected target ${app.packageName}")
        refreshLogs()
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
    }

    fun updateKillBeforeLaunch(value: Boolean) {
        _uiState.update { it.copy(injection = it.injection.copy(shouldKillBeforeLaunch = value)) }
    }

    fun updateRemap(value: Boolean) {
        _uiState.update { it.copy(injection = it.injection.copy(remapLibrary = value)) }
    }

    fun updateUseProxy(value: Boolean) {
        _uiState.update { it.copy(injection = it.injection.copy(useProxy = value)) }
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
    }

    fun updateBypassRestrictions(value: Boolean) {
        if (value && Utility.isEmulator()) {
            LogManager.AddLog("Emulator detected, namespace bypass disabled")
            refreshLogs()
            return
        }
        _uiState.update { it.copy(injection = it.injection.copy(bypassNamespaceRestrictions = value)) }
    }

    fun updateSettings(block: (SettingsUiState) -> SettingsUiState) {
        _uiState.update { it.copy(settings = block(it.settings)) }
    }

    fun clearLogs() {
        LogManager.logs.clear()
        refreshLogs()
    }

    fun copyLogsText(): String = uiState.value.logs.joinToString(separator = "\n")

    fun clearInjectionCache(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val payloadDir = File(appContext.cacheDir, "payloads")
            val deletedLocal = payloadDir.listFiles()?.count {
                it.isFile && it.delete()
            } ?: 0

            val rootMessage = if (RootManager.instance?.hasRootAccess == true) {
                val rootCleared = runCatching {
                    RootManager.instance?.clearInjectionCache() == true
                }.getOrDefault(false)
                if (rootCleared) {
                    "root temp cleared"
                } else {
                    "root temp not cleared"
                }
            } else {
                "root not granted"
            }

            _uiState.update {
                it.copy(
                    injection = it.injection.copy(
                        libraryPath = "",
                        payloadLabel = "",
                        processId = if (it.injection.packageName.isBlank()) "-1" else it.injection.processId
                    ),
                    status = StatusBanner(
                        title = "Injection cache cleared",
                        detail = "Removed $deletedLocal local payload file(s), $rootMessage.",
                        isError = false
                    )
                )
            }
            LogManager.AddLog("Injection cache cleared: local=$deletedLocal, $rootMessage")
            refreshLogs()
            withContext(Dispatchers.Main) {
                onResult("Removed $deletedLocal local payload file(s), $rootMessage")
            }
        }
    }

    fun stagePayload(
        resolver: ContentResolver,
        uri: Uri,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val staged = runCatching { copyPayloadToCache(appContext, resolver, uri) }
            staged.onSuccess { payload ->
                _uiState.update {
                    val nextType = if (payload.path.endsWith(".dex", true)) 1 else it.injection.injectType
                    it.copy(
                        injection = it.injection.copy(
                            libraryPath = payload.path,
                            payloadLabel = payload.displayName,
                            injectType = nextType
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    onResult("Payload staged: ${payload.displayName}")
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    onResult("Failed to read selected file")
                }
            }
        }
    }

    fun startInjection(
        activity: Activity,
        rootHandler: RootHandler,
        onResult: (String) -> Unit
    ) {
        val state = uiState.value.injection
        if (state.packageName.isBlank()) {
            onResult("Please select a target package")
            return
        }
        if (state.libraryPath.isBlank()) {
            onResult("Please select a payload file")
            return
        }
        if (!File(state.libraryPath).exists()) {
            onResult("Staged payload is missing, reselect the file")
            return
        }
        if (RootManager.instance?.hasRootAccess != true) {
            onResult("Root access not granted yet")
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

        _uiState.update {
            it.copy(
                injection = state.copy(isInjecting = true),
                status = StatusBanner(
                    title = "Injection running",
                    detail = "Dispatching request to the root service",
                    isError = false
                )
            )
        }
        LogManager.AddLog("Starting injection for ${state.packageName}")
        LogManager.AddLog("Payload ${state.libraryPath}")
        refreshLogs()

        rootHandler.Inject(activity, data) { success, message, logs ->
            viewModelScope.launch(Dispatchers.Main) {
                if (logs.isNotEmpty()) {
                    LogManager.logs.addAll(logs)
                }
                _uiState.update { current ->
                    current.copy(
                        injection = current.injection.copy(isInjecting = false),
                        status = StatusBanner(
                            title = if (success) "Injection completed" else "Injection failed",
                            detail = message,
                            isError = !success
                        )
                    )
                }
                refreshLogs()
                refreshOverview()
                onResult(message)
            }
        }
    }

    private fun checkRootSolution(packageNames: Array<String>): String {
        val packageManager = appContext.packageManager
        for (packageName in packageNames) {
            try {
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    android.content.pm.PackageManager.GET_ACTIVITIES
                )
                val appInfo = packageInfo.applicationInfo ?: continue
                return packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
            }
        }

        val knownPaths = arrayOf(
            "/sbin/magisk",
            "/system/bin/magisk",
            "/system/xbin/magisk",
            "/data/adb/magisk",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su"
        )
        return if (knownPaths.any { File(it).exists() }) "Magisk or compatible" else "Not detected"
    }

    private fun checkZygiskStatusWithRoot(): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c ls /data/adb/modules")
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            if (output.contains("zygisksu") || output.contains("zygisk")) "Detected" else "Not detected"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    private fun checkSecurityStatusWithRoot(): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c getenforce")
            val status = process.inputStream.bufferedReader().use { it.readLine()?.trim().orEmpty() }
            process.waitFor()
            if (status.isNotEmpty()) "SELinux $status" else "SELinux Unknown"
        } catch (_: Exception) {
            "SELinux Unknown"
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(context.applicationContext) as T
            }
        }

        private suspend fun copyPayloadToCache(
            context: Context,
            resolver: ContentResolver,
            uri: Uri
        ): StagedPayload = withContext(Dispatchers.IO) {
            val payloadDir = File(context.cacheDir, "payloads").apply { mkdirs() }
            payloadDir.listFiles()?.forEach {
                if (it.isFile && System.currentTimeMillis() - it.lastModified() > 24L * 60L * 60L * 1000L) {
                    it.delete()
                }
            }

            val displayName = queryDisplayName(resolver, uri) ?: "payload-${System.currentTimeMillis()}"
            val extension = displayName.substringAfterLast('.', "")
            if (extension.lowercase() !in setOf("so", "dex")) {
                throw IllegalArgumentException("Unsupported file type")
            }

            val sanitized = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val stagedFile = File(payloadDir, "${System.currentTimeMillis()}-$sanitized")
            resolver.openInputStream(uri).use { input ->
                if (input == null) error("Unable to open input stream")
                FileOutputStream(stagedFile).use { output ->
                    input.copyTo(output)
                }
            }
            StagedPayload(displayName = displayName, path = stagedFile.absolutePath)
        }

        private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(index)
                }
            }
            return uri.lastPathSegment
        }
    }
}

private data class MainUiState(
    val currentTab: MainTab = MainTab.OVERVIEW,
    val overview: DeviceOverview = DeviceOverview(),
    val installedApps: List<AppEntry> = emptyList(),
    val searchQuery: String = "",
    val injection: InjectionUiState = InjectionUiState(),
    val logs: List<String> = emptyList(),
    val settings: SettingsUiState = SettingsUiState(),
    val status: StatusBanner? = null
)

private data class DeviceOverview(
    val systemVersion: String = "Loading...",
    val device: String = "Loading...",
    val primaryAbi: String = "Loading...",
    val isRooted: Boolean = false,
    val rootGranted: Boolean = false,
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
    val payloadLabel: String = "",
    val injectType: Int = 0,
    val dexClassName: String = "io.github.reveny.dex.Main",
    val dexMethodName: String = "main",
    val shouldAutoLaunch: Boolean = false,
    val shouldKillBeforeLaunch: Boolean = false,
    val remapLibrary: Boolean = false,
    val useProxy: Boolean = false,
    val randomizeProxyName: Boolean = false,
    val hideLibrary: Boolean = false,
    val bypassNamespaceRestrictions: Boolean = false,
    val isInjecting: Boolean = false
)

private data class SettingsUiState(
    val followSystemAccent: Boolean = App.getPreferences().getBoolean("follow_system_accent", true),
    val blackTheme: Boolean = App.getPreferences().getBoolean("black_dark_theme", false),
    val darkTheme: String = App.getPreferences().getString("dark_theme", ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM)
        ?: ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM,
    val language: String = App.getPreferences().getString("language", "SYSTEM") ?: "SYSTEM",
    val themeColor: String = App.getPreferences().getString("theme_color", "COLOR_BLUE") ?: "COLOR_BLUE"
)

private data class StatusBanner(
    val title: String,
    val detail: String,
    val isError: Boolean
)

private data class StagedPayload(
    val displayName: String,
    val path: String
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

private enum class MainTab(val labelRes: Int, val icon: ImageVector) {
    OVERVIEW(R.string.tab_overview, Icons.Outlined.Info),
    INJECTION(R.string.tab_injection, Icons.Outlined.Bolt),
    LOGS(R.string.tab_logs, Icons.AutoMirrored.Outlined.Article),
    SETTINGS(R.string.tab_settings, Icons.Outlined.Settings)
}

@Composable
private fun MainRoute(viewModel: MainViewModel, rootHandler: RootHandler) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        viewModel.stagePayload(context.contentResolver, uri) { message ->
            scope.launch { snackbars.showSnackbar(message) }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            MainTopBar(
                currentTab = uiState.currentTab,
                onRefresh = { viewModel.refreshAll() }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = uiState.currentTab == tab,
                        onClick = { viewModel.switchTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                MainTab.OVERVIEW -> OverviewScreen(
                    overview = uiState.overview,
                    status = uiState.status,
                    onJumpToInject = { viewModel.switchTab(MainTab.INJECTION) }
                )
                MainTab.INJECTION -> InjectionScreen(
                    state = uiState,
                    onSearchChange = viewModel::updateSearch,
                    onSelectApp = viewModel::selectApp,
                    onPickLibrary = { filePicker.launch("*/*") },
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
                        viewModel.startInjection(context as Activity, rootHandler) { message ->
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
                    onClearCache = {
                        viewModel.clearInjectionCache { message ->
                            scope.launch { snackbars.showSnackbar(message) }
                        }
                    },
                    onApply = { message ->
                        scope.launch { snackbars.showSnackbar(message) }
                    }
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
            Text(stringResource(currentTab.labelRes), fontWeight = FontWeight.SemiBold)
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewScreen(
    overview: DeviceOverview,
    status: StatusBanner?,
    onJumpToInject: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionCard("Workspace", Icons.Outlined.Info) {
                Text(
                    "统一的 Compose 注入工作台，管理目标应用、payload、root 状态和运行日志。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Version ${BuildConfig.VERSION_NAME}") },
                        leadingIcon = { Icon(Icons.Outlined.Android, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(if (overview.rootGranted) "Root Ready" else "Root Pending") },
                        leadingIcon = { Icon(Icons.Outlined.Shield, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(overview.primaryAbi) },
                        leadingIcon = { Icon(Icons.Outlined.Memory, contentDescription = null) }
                    )
                }
                OutlinedButton(onClick = onJumpToInject) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Injection")
                }
            }
        }

        if (status != null) {
            item {
                StatusCard(status)
            }
        }

        item {
            SectionCard("Device Snapshot", Icons.Outlined.Security) {
                OverviewRow(Icons.Outlined.Android, "System", overview.systemVersion)
                OverviewRow(Icons.Outlined.AppShortcut, "Device", overview.device)
                OverviewRow(Icons.Outlined.Memory, "Primary ABI", overview.primaryAbi)
                OverviewRow(Icons.Outlined.Shield, "Root Binary", if (overview.isRooted) "Detected" else "Missing")
                OverviewRow(Icons.Outlined.CheckCircle, "Root Session", if (overview.rootGranted) "Granted" else "Waiting")
                OverviewRow(Icons.Outlined.Security, "Root Stack", overview.rootSystem)
                OverviewRow(Icons.Outlined.Bolt, "Zygisk", overview.zygiskStatus)
                OverviewRow(Icons.Outlined.Apps, "Environment", if (overview.isEmulator) "Emulator" else "Physical")
                OverviewRow(Icons.Outlined.WarningAmber, "Security", overview.securityStatus, isLast = true)
            }
        }
    }
}

@Composable
private fun StatusCard(status: StatusBanner) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isError) Color(0xFFFFECE8) else Color(0xFFEAF8F0)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (status.isError) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (status.isError) Color(0xFFAF3D28) else Color(0xFF226C43)
                )
                Text(status.title, fontWeight = FontWeight.SemiBold)
            }
            Text(status.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OverviewRow(
    icon: ImageVector,
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (!isLast) {
        Spacer(Modifier.height(6.dp))
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
        if (state.searchQuery.isBlank()) {
            state.installedApps
        } else {
            state.installedApps.filter {
                "${it.label} ${it.packageName}".lowercase().contains(state.searchQuery.lowercase())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard("Injection Workflow", Icons.Outlined.Bolt) {
                    Text(
                        "文件会先暂存到应用私有缓存，再由 root/native 层复制到受控目录执行。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SectionCard("Target", Icons.Outlined.Apps) {
                    ReadonlyField(
                        label = "Target App",
                        value = if (state.injection.packageName.isBlank()) {
                            "Tap to choose an installed app"
                        } else {
                            "${state.injection.selectedAppLabel} (${state.injection.packageName})"
                        },
                        buttonLabel = "Browse",
                        onClick = { appSheetOpen = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    TripleStatRow(
                        "Package" to state.injection.packageName.ifBlank { "Not selected" },
                        "PID" to state.injection.processId,
                        "UID" to if (state.injection.appUid >= 0) state.injection.appUid.toString() else "-"
                    )
                }
            }

            item {
                SectionCard("Payload", Icons.Outlined.FolderOpen) {
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
                        value = state.injection.payloadLabel.ifBlank { "No payload selected" },
                        supporting = state.injection.libraryPath.ifBlank { "Payload will be staged into app cache first" },
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
                                supportingText = { Text("默认会尝试调用 static method(Context)、static method() 或 main(String[])") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.injection.dexMethodName,
                                onValueChange = onDexMethodChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("DEX Method Name") },
                                supportingText = { Text("例如 main 或 initialize") },
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item {
                SectionCard("Execution Policy", Icons.Outlined.Shield) {
                    ToggleRow("Auto Launch", state.injection.shouldAutoLaunch, onAutoLaunchChange)
                    ToggleRow("Kill Before Auto Launch", state.injection.shouldKillBeforeLaunch, onKillBeforeLaunchChange)
                    AnimatedVisibility(visible = state.injection.injectType == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Spacer(Modifier.height(4.dp))
                            ToggleRow("Remap Library", state.injection.remapLibrary, onRemapChange)
                            ToggleRow("Use Proxy Library", state.injection.useProxy, onUseProxyChange)
                            ToggleRow("Randomize Proxy Name", state.injection.randomizeProxyName, onRandomizeProxyChange)
                            ToggleRow("Hide Loaded Library", state.injection.hideLibrary, onHideLibraryChange)
                            ToggleRow("Bypass Namespace Restrictions", state.injection.bypassNamespaceRestrictions, onBypassChange)
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Terminal, contentDescription = null)
                            Text("Launch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "确认目标应用和 payload 后再启动注入。运行期间会持续刷新状态和日志。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.injection.isInjecting) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        OutlinedButton(
                            onClick = onStartInjection,
                            enabled = !state.injection.isInjecting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Outlined.Terminal, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(if (state.injection.isInjecting) "Injection Running" else "Start Injection")
                        }
                    }
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
                            Icon(Icons.Outlined.AppShortcut, contentDescription = null)
                        }
                    }
                }
            }
        }
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
            SectionCard("Runtime Logs", Icons.AutoMirrored.Outlined.Article) {
                Text(
                    "Java 与 native 日志会合并显示，便于排查 root service、payload staging 和注入阶段的问题。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AssistChip(onClick = onCopy, label = { Text("Copy") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) })
                    AssistChip(onClick = onClear, label = { Text("Clear") }, leadingIcon = { Icon(Icons.Outlined.WarningAmber, null) })
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No logs yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("触发一次刷新或注入后，这里会出现新的运行日志。")
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
    onClearCache: () -> Unit,
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
            SectionCard("Preferences", Icons.Outlined.Settings) {
                Text(
                    "保留原有 SharedPreferences 键，改成标准 MD3 交互，不破坏兼容性。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onClearCache) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Injection Cache")
                }
            }
        }

        item {
            SectionCard("Language", Icons.Outlined.Translate) {
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
                            label = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(localizedText(context, option.tag, option.labelRes))
                                }
                            }
                        )
                    }
                }
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Current", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(languageDisplayName(context, state.language), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
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
            SectionCard("Theme", Icons.Outlined.Palette) {
                ToggleRow("Follow system accent", state.followSystemAccent) { value ->
                    preferences.edit().putBoolean("follow_system_accent", value).apply()
                    onUpdate { it.copy(followSystemAccent = value) }
                    onApply("Accent preference updated. Restart if overlays do not refresh.")
                }
                ToggleRow("Pure black dark theme", state.blackTheme) { value ->
                    preferences.edit().putBoolean("black_dark_theme", value).apply()
                    onUpdate { it.copy(blackTheme = value) }
                    onApply("Dark theme preference updated.")
                }
                Text("Dark mode", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    darkThemeOptions.forEach { (label, value) ->
                        FilterChip(
                            selected = state.darkTheme == value,
                            onClick = {
                                preferences.edit().putString("dark_theme", value).apply()
                                onUpdate { it.copy(darkTheme = value) }
                                onApply("Dark mode updated.")
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
                                onApply("Theme color updated.")
                            },
                            label = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(label)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun ReadonlyField(
    label: String,
    value: String,
    supporting: String? = null,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Text(value, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = onClick) { Text(buttonLabel) }
                }
                if (!supporting.isNullOrBlank()) {
                    Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TripleStatRow(
    first: Pair<String, String>,
    second: Pair<String, String>,
    third: Pair<String, String>
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(first, second, third).forEach { (label, value) ->
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

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

@Composable
private fun InjectorComposeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val useDark = when (ThemeUtil.getDarkTheme()) {
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> true
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> isSystemInDarkTheme()
    }

    val light = lightColorScheme(
        primary = Color(0xFF4B5D92),
        secondary = Color(0xFF5E5F71),
        tertiary = Color(0xFF75546B),
        background = Color(0xFFFBF8FD),
        surface = Color(0xFFFFFBFF)
    )
    val dark = darkColorScheme(
        primary = Color(0xFFB4C4FF),
        secondary = Color(0xFFC6C5DA),
        tertiary = Color(0xFFE3BAD7),
        background = Color(0xFF131318),
        surface = Color(0xFF1B1B20)
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
