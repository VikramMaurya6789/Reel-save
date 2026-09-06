package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReelSaveApplication
import com.example.data.local.AppSettings
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val storageUsedFormatted: String = "Calculating...",
    val showClearHistoryDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val showTermsDialog: Boolean = false,
    val showLicensesDialog: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as ReelSaveApplication).container
    private val preferencesManager = container.preferencesManager
    private val repository = container.repository

    private val _storageUsed = MutableStateFlow("Calculating...")
    private val _dialogState = MutableStateFlow(
        DialogState(
            showClearHistory = false,
            showPrivacy = false,
            showTerms = false,
            showLicenses = false
        )
    )

    private data class DialogState(
        val showClearHistory: Boolean,
        val showPrivacy: Boolean,
        val showTerms: Boolean,
        val showLicenses: Boolean
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.settings,
        _storageUsed,
        _dialogState
    ) { settings, storage, dialogs ->
        SettingsUiState(
            settings = settings,
            storageUsedFormatted = storage,
            showClearHistoryDialog = dialogs.showClearHistory,
            showPrivacyDialog = dialogs.showPrivacy,
            showTermsDialog = dialogs.showTerms,
            showLicensesDialog = dialogs.showLicenses
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        calculateStorageUsed()
    }

    fun calculateStorageUsed() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheSize = getDirSize(getApplication<Application>().cacheDir)
            val externalCache = getApplication<Application>().externalCacheDir?.let { getDirSize(it) } ?: 0L
            val totalBytes = cacheSize + externalCache
            val formatted = FileUtils.formatFileSize(totalBytes)
            withContext(Dispatchers.Main) {
                _storageUsed.value = formatted
            }
        }
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().cacheDir.deleteRecursively()
            getApplication<Application>().externalCacheDir?.deleteRecursively()
            calculateStorageUsed()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _dialogState.value = _dialogState.value.copy(showClearHistory = false)
        }
    }

    fun updateDefaultQuality(quality: String) = preferencesManager.updateDefaultQuality(quality)
    fun updateAutoSave(enabled: Boolean) = preferencesManager.updateAutoSave(enabled)
    fun updateWifiOnly(enabled: Boolean) = preferencesManager.updateWifiOnly(enabled)
    fun updateClipboardDetection(enabled: Boolean) = preferencesManager.updateClipboardDetection(enabled)
    fun updateConfirmBeforeDownload(enabled: Boolean) = preferencesManager.updateConfirmBeforeDownload(enabled)
    fun updateOpenAutomatically(enabled: Boolean) = preferencesManager.updateOpenAutomatically(enabled)
    fun updateThemeMode(mode: String) = preferencesManager.updateThemeMode(mode)

    fun showClearHistoryDialog(show: Boolean) {
        _dialogState.value = _dialogState.value.copy(showClearHistory = show)
    }

    fun showPrivacyDialog(show: Boolean) {
        _dialogState.value = _dialogState.value.copy(showPrivacy = show)
    }

    fun showTermsDialog(show: Boolean) {
        _dialogState.value = _dialogState.value.copy(showTerms = show)
    }

    fun showLicensesDialog(show: Boolean) {
        _dialogState.value = _dialogState.value.copy(showLicenses = show)
    }
}
