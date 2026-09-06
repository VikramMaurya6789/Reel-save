package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val defaultQuality: String = "Original",
    val saveLocation: String = "Movies/ReelSave",
    val autoSaveToGallery: Boolean = true,
    val wifiOnly: Boolean = false,
    val maxConcurrentDownloads: Int = 2,
    val clipboardDetection: Boolean = true,
    val confirmBeforeDownload: Boolean = false,
    val openAutomatically: Boolean = false,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val hasCompletedOnboarding: Boolean = true
)

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("reelsave_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            defaultQuality = prefs.getString("default_quality", "Original") ?: "Original",
            saveLocation = prefs.getString("save_location", "Movies/ReelSave") ?: "Movies/ReelSave",
            autoSaveToGallery = prefs.getBoolean("auto_save_gallery", true),
            wifiOnly = prefs.getBoolean("wifi_only", false),
            maxConcurrentDownloads = prefs.getInt("concurrent_downloads", 2),
            clipboardDetection = prefs.getBoolean("clipboard_detection", true),
            confirmBeforeDownload = prefs.getBoolean("confirm_before_download", false),
            openAutomatically = prefs.getBoolean("open_automatically", false),
            themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM",
            hasCompletedOnboarding = prefs.getBoolean("onboarding_complete", true)
        )
    }

    fun updateDefaultQuality(quality: String) {
        prefs.edit().putString("default_quality", quality).apply()
        _settings.value = _settings.value.copy(defaultQuality = quality)
    }

    fun updateClipboardDetection(enabled: Boolean) {
        prefs.edit().putBoolean("clipboard_detection", enabled).apply()
        _settings.value = _settings.value.copy(clipboardDetection = enabled)
    }

    fun updateWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean("wifi_only", enabled).apply()
        _settings.value = _settings.value.copy(wifiOnly = enabled)
    }

    fun updateAutoSave(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_gallery", enabled).apply()
        _settings.value = _settings.value.copy(autoSaveToGallery = enabled)
    }

    fun updateConfirmBeforeDownload(enabled: Boolean) {
        prefs.edit().putBoolean("confirm_before_download", enabled).apply()
        _settings.value = _settings.value.copy(confirmBeforeDownload = enabled)
    }

    fun updateOpenAutomatically(enabled: Boolean) {
        prefs.edit().putBoolean("open_automatically", enabled).apply()
        _settings.value = _settings.value.copy(openAutomatically = enabled)
    }

    fun updateThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun updateMaxConcurrent(count: Int) {
        prefs.edit().putInt("concurrent_downloads", count).apply()
        _settings.value = _settings.value.copy(maxConcurrentDownloads = count)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_complete", completed).apply()
        _settings.value = _settings.value.copy(hasCompletedOnboarding = completed)
    }
}
