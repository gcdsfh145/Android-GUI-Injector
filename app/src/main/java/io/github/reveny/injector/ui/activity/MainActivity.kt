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
            LogManager.AddLog(appContext.getString(R.string.message_emulator_bypass_disabled))
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
                    appContext.getString(R.string.cache_root_cleared)
                } else {
                    appContext.getString(R.string.cache_root_not_cleared)
                }
            } else {
                appContext.getString(R.string.cache_root_not_granted)
            }

            _uiState.update {
                it.copy(
                    injection = it.injection.copy(
                        libraryPath = "",
                        payloadLabel = "",
                        processId = if (it.injection.packageName.isBlank()) "-1" else it.injection.processId
                    ),
                    status = StatusBanner(
                        title = appContext.getString(R.string.status_cache_cleared),
                        detail = appContext.getString(R.string.status_cache_cleared_detail, deletedLocal, rootMessage),
                        isError = false
                    )
                )
            }
            LogManager.AddLog("Injection cache cleared: local=$deletedLocal, $rootMessage")
            refreshLogs()
            withContext(Dispatchers.Main) {
                onResult(appContext.getString(R.string.status_cache_cleared_detail, deletedLocal, rootMessage))
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
                    onResult(appContext.getString(R.string.message_payload_staged, payload.displayName))
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    onResult(appContext.getString(R.string.message_failed_read_selected_file))
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
            onResult(appContext.getString(R.string.message_select_target_package))
            return
        }
        if (state.libraryPath.isBlank()) {
            onResult(appContext.getString(R.string.message_select_payload_file))
            return
        }
        if (!File(state.libraryPath).exists()) {
            onResult(appContext.getString(R.string.message_staged_payload_missing))
            return
        }
        if (RootManager.instance?.hasRootAccess != true) {
            onResult(appContext.getString(R.string.message_root_not_granted))
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
                    title = appContext.getString(R.string.status_injection_running),
                    detail = appContext.getString(R.string.status_dispatching_root_service),
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
                            title = if (success) appContext.getString(R.string.status_injection_completed)
                            else appContext.getString(R.string.status_injection_failed),
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
        return if (knownPaths.any { File(it).exists() }) {
            appContext.getString(R.string.value_magisk_compatible)
        } else {
            appContext.getString(R.string.value_not_detected)
        }
    }

    private fun checkZygiskStatusWithRoot(): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c ls /data/adb/modules")
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            if (output.contains("zygisksu") || output.contains("zygisk")) {
                appContext.getString(R.string.state_detected)
            } else {
                appContext.getString(R.string.value_not_detected)
            }
        } catch (_: Exception) {
            appContext.getString(R.string.value_unknown)
        }
    }

    private fun checkSecurityStatusWithRoot(): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c getenforce")
            val status = process.inputStream.bufferedReader().use { it.readLine()?.trim().orEmpty() }
            process.waitFor()
            if (status.isNotEmpty()) "SELinux $status" else appContext.getString(R.string.value_selinux_unknown)
        } catch (_: Exception) {
            appContext.getString(R.string.value_selinux_unknown)
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
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                            android.content.ClipData.newPlainText(
                                context.getString(R.string.tab_logs),
                                viewModel.copyLogsText()
                            )
                        )
                        scope.launch { snackbars.showSnackbar(context.getString(R.string.message_logs_copied)) }
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.workspace_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(currentTab.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            HeroCard(
                title = stringResource(R.string.workspace_title),
                subtitle = stringResource(R.string.workspace_description),
                actionLabel = stringResource(R.string.action_open_injection),
                onAction = onJumpToInject
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.workspace_version, BuildConfig.VERSION_NAME)) },
                        leadingIcon = { Icon(Icons.Outlined.Android, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(if (overview.rootGranted) R.string.workspace_root_ready else R.string.workspace_root_pending)) },
                        leadingIcon = { Icon(Icons.Outlined.Shield, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(overview.primaryAbi) },
                        leadingIcon = { Icon(Icons.Outlined.Memory, contentDescription = null) }
                    )
                }
            }
        }

        if (status != null) {
            item {
                StatusCard(status)
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.section_device_snapshot),
                icon = Icons.Outlined.Security
            ) {
                OverviewRow(Icons.Outlined.Android, stringResource(R.string.snapshot_system), overview.systemVersion)
                OverviewRow(Icons.Outlined.AppShortcut, stringResource(R.string.snapshot_device), overview.device)
                OverviewRow(Icons.Outlined.Memory, stringResource(R.string.snapshot_primary_abi), overview.primaryAbi)
                OverviewRow(
                    Icons.Outlined.Shield,
                    stringResource(R.string.snapshot_root_binary),
                    stringResource(if (overview.isRooted) R.string.state_detected else R.string.state_missing)
                )
                OverviewRow(
                    Icons.Outlined.CheckCircle,
                    stringResource(R.string.snapshot_root_session),
                    stringResource(if (overview.rootGranted) R.string.state_granted else R.string.state_waiting)
                )
                OverviewRow(Icons.Outlined.Security, stringResource(R.string.snapshot_root_stack), overview.rootSystem)
                OverviewRow(Icons.Outlined.Bolt, stringResource(R.string.snapshot_zygisk), overview.zygiskStatus)
                OverviewRow(
                    Icons.Outlined.Apps,
                    stringResource(R.string.snapshot_environment),
                    stringResource(if (overview.isEmulator) R.string.state_emulator else R.string.state_physical)
                )
                OverviewRow(Icons.Outlined.WarningAmber, stringResource(R.string.snapshot_security), overview.securityStatus, isLast = true)
            }
        }
    }
}

@Composable
private fun StatusCard(status: StatusBanner) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
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
                    tint = if (status.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    status.title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (status.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                status.detail,
                color = if (status.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f)
                )
            }
            content()
            OutlinedButton(onClick = onAction) {
                Icon(Icons.Outlined.Bolt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(actionLabel)
            }
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
                HeroCard(
                    title = stringResource(R.string.section_injection_workflow),
                    subtitle = stringResource(R.string.label_payload_workflow_description),
                    actionLabel = stringResource(R.string.action_pick_file),
                    onAction = onPickLibrary
                ) {
                    if (state.injection.payloadLabel.isNotBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text(state.injection.payloadLabel) },
                            leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) }
                        )
                    }
                }
            }

            item {
                SectionCard(title = stringResource(R.string.section_target), icon = Icons.Outlined.Apps) {
                    ReadonlyField(
                        label = stringResource(R.string.label_target_app),
                        value = if (state.injection.packageName.isBlank()) {
                            stringResource(R.string.label_target_app_placeholder)
                        } else {
                            "${state.injection.selectedAppLabel} (${state.injection.packageName})"
                        },
                        buttonLabel = stringResource(R.string.action_browse),
                        onClick = { appSheetOpen = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    TripleStatRow(
                        stringResource(R.string.label_package) to state.injection.packageName.ifBlank { "-" },
                        stringResource(R.string.label_pid) to state.injection.processId,
                        stringResource(R.string.label_uid) to if (state.injection.appUid >= 0) state.injection.appUid.toString() else "-"
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.section_payload), icon = Icons.Outlined.FolderOpen) {
                    TabRow(selectedTabIndex = state.injection.injectType) {
                        listOf(
                            stringResource(R.string.label_shared_library),
                            stringResource(R.string.label_dex_entry)
                        ).forEachIndexed { index, title ->
                            Tab(
                                selected = state.injection.injectType == index,
                                onClick = { onInjectTypeChange(index) },
                                text = { Text(title) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ReadonlyField(
                        label = if (state.injection.injectType == 0) {
                            stringResource(R.string.label_library_path)
                        } else {
                            stringResource(R.string.label_dex_path)
                        },
                        value = state.injection.payloadLabel.ifBlank { stringResource(R.string.label_no_payload_selected) },
                        supporting = state.injection.libraryPath.ifBlank { stringResource(R.string.label_payload_staging_note) },
                        buttonLabel = stringResource(R.string.action_pick_file),
                        onClick = onPickLibrary
                    )
                    AnimatedVisibility(visible = state.injection.injectType == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = state.injection.dexClassName,
                                onValueChange = onDexClassChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.label_dex_class_name)) },
                                supportingText = { Text(stringResource(R.string.label_dex_class_hint)) },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.injection.dexMethodName,
                                onValueChange = onDexMethodChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.label_dex_method_name)) },
                                supportingText = { Text(stringResource(R.string.label_dex_method_hint)) },
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = stringResource(R.string.section_execution_policy), icon = Icons.Outlined.Shield) {
                    ToggleRow(stringResource(R.string.toggle_auto_launch), state.injection.shouldAutoLaunch, onAutoLaunchChange)
                    ToggleRow(stringResource(R.string.toggle_kill_before_launch), state.injection.shouldKillBeforeLaunch, onKillBeforeLaunchChange)
                    AnimatedVisibility(visible = state.injection.injectType == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Spacer(Modifier.height(4.dp))
                            ToggleRow(stringResource(R.string.toggle_remap_library), state.injection.remapLibrary, onRemapChange)
                            ToggleRow(stringResource(R.string.toggle_use_proxy), state.injection.useProxy, onUseProxyChange)
                            ToggleRow(stringResource(R.string.toggle_randomize_proxy), state.injection.randomizeProxyName, onRandomizeProxyChange)
                            ToggleRow(stringResource(R.string.toggle_hide_library), state.injection.hideLibrary, onHideLibraryChange)
                            ToggleRow(stringResource(R.string.toggle_bypass_namespace), state.injection.bypassNamespaceRestrictions, onBypassChange)
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            Text(stringResource(R.string.section_launch), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            stringResource(R.string.label_launch_description),
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
                            Text(
                                stringResource(
                                    if (state.injection.isInjecting) R.string.action_injection_running
                                    else R.string.action_start_injection
                                )
                            )
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
            Text(stringResource(R.string.label_select_target_app), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_search_apps)) },
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
            SectionCard(title = stringResource(R.string.section_runtime_logs), icon = Icons.AutoMirrored.Outlined.Article) {
                Text(
                    stringResource(R.string.logs_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AssistChip(onClick = onCopy, label = { Text(stringResource(R.string.action_copy)) }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) })
                    AssistChip(onClick = onClear, label = { Text(stringResource(R.string.action_clear)) }, leadingIcon = { Icon(Icons.Outlined.WarningAmber, null) })
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.logs_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.logs_empty_summary))
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
        R.string.theme_option_light to ThemeUtil.MODE_NIGHT_NO,
        R.string.theme_option_dark to ThemeUtil.MODE_NIGHT_YES,
        R.string.theme_option_system to ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM
    )
    val colorOptions = listOf(
        R.string.color_sakura to "SAKURA",
        R.string.color_blue to "MATERIAL_BLUE",
        R.string.color_teal to "MATERIAL_TEAL",
        R.string.color_green to "MATERIAL_GREEN",
        R.string.color_amber to "MATERIAL_AMBER"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionCard(title = stringResource(R.string.section_preferences), icon = Icons.Outlined.Settings) {
                Text(
                    stringResource(R.string.settings_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.settings_cache_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onClearCache) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_clear_injection_cache))
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_language), icon = Icons.Outlined.Translate) {
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
                        Text(stringResource(R.string.language_current_short), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            SectionCard(title = stringResource(R.string.section_theme), icon = Icons.Outlined.Palette) {
                ToggleRow(stringResource(R.string.theme_follow_system_accent), state.followSystemAccent) { value ->
                    preferences.edit().putBoolean("follow_system_accent", value).apply()
                    onUpdate { it.copy(followSystemAccent = value) }
                    onApply(context.getString(R.string.theme_follow_system_accent_message))
                }
                ToggleRow(stringResource(R.string.theme_pure_black), state.blackTheme) { value ->
                    preferences.edit().putBoolean("black_dark_theme", value).apply()
                    onUpdate { it.copy(blackTheme = value) }
                    onApply(context.getString(R.string.theme_pure_black_message))
                }
                Text(stringResource(R.string.theme_dark_mode), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    darkThemeOptions.forEach { (labelRes, value) ->
                        FilterChip(
                            selected = state.darkTheme == value,
                            onClick = {
                                preferences.edit().putString("dark_theme", value).apply()
                                onUpdate { it.copy(darkTheme = value) }
                                onApply(context.getString(R.string.theme_dark_mode_message))
                            },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
                Text(stringResource(R.string.theme_accent), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { (labelRes, value) ->
                        FilterChip(
                            selected = state.themeColor == value,
                            onClick = {
                                preferences.edit().putString("theme_color", value).apply()
                                onUpdate { it.copy(themeColor = value) }
                                onApply(context.getString(R.string.theme_color_message))
                            },
                            label = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(stringResource(labelRes))
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    Text(value, modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = onClick) { Text(buttonLabel) }
                }
                if (!supporting.isNullOrBlank()) {
                    Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TripleStatRow(
    first: Pair<String, String>,
    second: Pair<String, String>,
    third: Pair<String, String>
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf(first, second, third).forEach { (label, value) ->
            OutlinedCard(modifier = Modifier.width(160.dp)) {
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
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
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
        primary = Color(0xFF5B5FC7),
        secondary = Color(0xFF625B71),
        tertiary = Color(0xFF7D5260),
        background = Color(0xFFF9F6FF),
        surface = Color(0xFFFFFBFF)
    )
    val dark = darkColorScheme(
        primary = Color(0xFFC2C3FF),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8),
        background = Color(0xFF121218),
        surface = Color(0xFF1C1B22)
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
