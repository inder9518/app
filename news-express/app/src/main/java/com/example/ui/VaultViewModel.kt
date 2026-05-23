package com.example.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HiddenApp
import com.example.data.NewsItem
import com.example.data.NewsProvider
import com.example.data.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VaultScreen {
    NEWS,
    PASSCODE_ENTRY,
    PASSCODE_SETUP,
    APPS_VAULT
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean = false
)

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {

    // Main Navigation State
    private val _currentScreen = MutableStateFlow(VaultScreen.NEWS)
    val currentScreen: StateFlow<VaultScreen> = _currentScreen.asStateFlow()

    // News Pagination (0 to 4 for 50 articles, 10 per page)
    private val _newsPage = MutableStateFlow(0)
    val newsPage: StateFlow<Int> = _newsPage.asStateFlow()

    val currentNews: StateFlow<List<NewsItem>> = _newsPage
        .map { page ->
            val pageSize = 10
            val startIdx = page * pageSize
            val endIdx = (startIdx + pageSize).coerceAtMost(NewsProvider.allNews.size)
            if (startIdx in NewsProvider.allNews.indices) {
                NewsProvider.allNews.subList(startIdx, endIdx)
            } else {
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Passcode States
    private val _inputPasscode = MutableStateFlow("")
    val inputPasscode: StateFlow<String> = _inputPasscode.asStateFlow()

    private val _passcodeError = MutableStateFlow<String?>(null)
    val passcodeError: StateFlow<String?> = _passcodeError.asStateFlow()

    private val _tempSetupPasscode = MutableStateFlow("")
    private val _setupStep = MutableStateFlow(1) // 1 = Enter Code, 2 = Confirm Code
    val setupStep: StateFlow<Int> = _setupStep.asStateFlow()

    private val _savedPasscode = MutableStateFlow<String?>(null)
    val savedPasscode: StateFlow<String?> = _savedPasscode.asStateFlow()

    // Vault Apps States
    val hiddenApps: StateFlow<List<HiddenApp>> = repository.hiddenApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(_installedApps, _searchQuery, hiddenApps) { list, query, hidden ->
        val hiddenPackages = hidden.map { it.packageName }.toSet()
        list.filter { app ->
            // Exclude already added apps
            !hiddenPackages.contains(app.packageName) &&
            // Filter by search query
            (app.label.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAddingDialogVisible = MutableStateFlow(false)
    val isAddingDialogVisible: StateFlow<Boolean> = _isAddingDialogVisible.asStateFlow()

    init {
        checkSavedPasscode()
    }

    private fun checkSavedPasscode() {
        viewModelScope.launch {
            val code = repository.getPasscode()
            _savedPasscode.value = code
        }
    }

    // Navigation trigger methods
    fun navigateTo(screen: VaultScreen) {
        _currentScreen.value = screen
        _inputPasscode.value = ""
        _passcodeError.value = null
        _tempSetupPasscode.value = ""
        _setupStep.value = 1
    }

    fun handleRefreshClick() {
        val nextPage = (_newsPage.value + 1) % 5
        _newsPage.value = nextPage
    }

    // Call this when long pressing the refresh button for 3 seconds
    fun handleRefreshLongPress() {
        checkSavedPasscode()
        viewModelScope.launch {
            val code = repository.getPasscode()
            if (code.isNullOrEmpty()) {
                navigateTo(VaultScreen.PASSCODE_SETUP)
            } else {
                navigateTo(VaultScreen.PASSCODE_ENTRY)
            }
        }
    }

    // Digit Pad Press Inputs
    fun onDigitPress(digit: String) {
        _passcodeError.value = null
        if (_currentScreen.value == VaultScreen.PASSCODE_ENTRY) {
            if (_inputPasscode.value.length < 4) {
                _inputPasscode.value += digit
                if (_inputPasscode.value.length == 4) {
                    verifyPasscode()
                }
            }
        } else if (_currentScreen.value == VaultScreen.PASSCODE_SETUP) {
            if (_inputPasscode.value.length < 4) {
                _inputPasscode.value += digit
                if (_inputPasscode.value.length == 4) {
                    processSetupPasscode()
                }
            }
        }
    }

    fun onBackspacePress() {
        _passcodeError.value = null
        if (_inputPasscode.value.isNotEmpty()) {
            _inputPasscode.value = _inputPasscode.value.dropLast(1)
        }
    }

    fun onClearPress() {
        _inputPasscode.value = ""
        _passcodeError.value = null
    }

    private fun verifyPasscode() {
        val entry = _inputPasscode.value
        val saved = _savedPasscode.value
        if (entry == saved) {
            _inputPasscode.value = ""
            navigateTo(VaultScreen.APPS_VAULT)
        } else {
            _inputPasscode.value = ""
            _passcodeError.value = "Galat Passcode! Koshish karein dobara."
        }
    }

    private fun processSetupPasscode() {
        val entry = _inputPasscode.value
        if (_setupStep.value == 1) {
            // Entered first code
            _tempSetupPasscode.value = entry
            _inputPasscode.value = ""
            _setupStep.value = 2
        } else if (_setupStep.value == 2) {
            // Confirming second code
            val firstCode = _tempSetupPasscode.value
            if (entry == firstCode) {
                viewModelScope.launch {
                    repository.savePasscode(entry)
                    _savedPasscode.value = entry
                    _inputPasscode.value = ""
                    _tempSetupPasscode.value = ""
                    navigateTo(VaultScreen.APPS_VAULT)
                }
            } else {
                _inputPasscode.value = ""
                _tempSetupPasscode.value = ""
                _setupStep.value = 1
                _passcodeError.value = "Passcode match nahi hua! Pehle se shuru karein."
            }
        }
    }

    // App Management Logic
    fun loadInstalledApps(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = packageManager.queryIntentActivities(intent, 0) ?: emptyList()
                val apps = resolveInfos.mapNotNull { resolveInfo ->
                    val activityInfo = resolveInfo?.activityInfo ?: return@mapNotNull null
                    val packageName = activityInfo.packageName ?: return@mapNotNull null
                    val label = resolveInfo.loadLabel(packageManager)?.toString() ?: packageName
                    val appInfo = activityInfo.applicationInfo
                    val isSystem = if (appInfo != null) {
                        (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    } else {
                        false
                    }
                    AppInfo(packageName, label, isSystem)
                }
                // Filter out current package name to avoid adding the Vault app itself recursively
                val myPackageName = "com.aistudio.newsexpress.hkbwpv"
                val filtered = apps.filter { it.packageName != myPackageName && it.packageName != "com.example" }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.uppercase() }

                _installedApps.value = filtered
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addAppToVault(packageName: String, appName: String) {
        viewModelScope.launch {
            repository.addHiddenApp(packageName, appName)
        }
    }

    fun removeAppFromVault(packageName: String) {
        viewModelScope.launch {
            repository.removeHiddenApp(packageName)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAddingDialogVisibility(visible: Boolean) {
        _isAddingDialogVisible.value = visible
        if (visible) {
            _searchQuery.value = ""
        }
    }
}
