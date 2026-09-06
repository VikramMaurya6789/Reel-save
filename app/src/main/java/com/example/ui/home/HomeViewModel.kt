package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReelSaveApplication
import com.example.domain.resolver.UrlValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UrlValidationStatus {
    EMPTY,
    VALID,
    INVALID
}

data class HomeUiState(
    val urlInput: String = "",
    val validationStatus: UrlValidationStatus = UrlValidationStatus.EMPTY,
    val detectedClipboardUrl: String? = null,
    val showClipboardBanner: Boolean = false,
    val showHowItWorksDialog: Boolean = false,
    val showOnboardingDialog: Boolean = false,
    val isResolving: Boolean = false,
    val errorMessage: String? = null
)

sealed class HomeUiEvent {
    data class NavigateToPreview(val url: String) : HomeUiEvent()
    data class ShowToast(val message: String) : HomeUiEvent()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as ReelSaveApplication).container
    private val preferencesManager = container.preferencesManager

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            preferencesManager.settings.collect { settings ->
                _uiState.update {
                    it.copy(showOnboardingDialog = !settings.hasCompletedOnboarding)
                }
            }
        }
    }

    fun onUrlInputChanged(input: String) {
        val trimmed = input.trim()
        val status = when {
            trimmed.isEmpty() -> UrlValidationStatus.EMPTY
            UrlValidator.isValidReelUrl(trimmed) -> UrlValidationStatus.VALID
            else -> UrlValidationStatus.INVALID
        }
        _uiState.update {
            it.copy(
                urlInput = input,
                validationStatus = status,
                errorMessage = null
            )
        }
    }

    fun onPaste(pastedText: String) {
        onUrlInputChanged(pastedText)
    }

    fun onClear() {
        _uiState.update {
            it.copy(
                urlInput = "",
                validationStatus = UrlValidationStatus.EMPTY,
                errorMessage = null
            )
        }
    }

    fun onClipboardDetected(url: String) {
        val settings = preferencesManager.settings.value
        if (!settings.clipboardDetection) return

        if (UrlValidator.isValidReelUrl(url)) {
            val normalized = UrlValidator.normalizeUrl(url) ?: url
            // Don't show if already pasted in input
            if (_uiState.value.urlInput.trim() != normalized) {
                _uiState.update {
                    it.copy(
                        detectedClipboardUrl = normalized,
                        showClipboardBanner = true
                    )
                }
            }
        }
    }

    fun onApplyClipboardUrl() {
        val detected = _uiState.value.detectedClipboardUrl ?: return
        onUrlInputChanged(detected)
        dismissClipboardBanner()
    }

    fun dismissClipboardBanner() {
        _uiState.update {
            it.copy(showClipboardBanner = false, detectedClipboardUrl = null)
        }
    }

    fun trySampleUrl() {
        val sampleUrl = "https://www.instagram.com/reel/sample/"
        onUrlInputChanged(sampleUrl)
    }

    fun onSubmitDownload() {
        val input = _uiState.value.urlInput.trim()
        if (!UrlValidator.isValidReelUrl(input)) {
            _uiState.update {
                it.copy(errorMessage = "Enter a valid Instagram Reel link.")
            }
            return
        }

        val normalized = UrlValidator.normalizeUrl(input) ?: input
        viewModelScope.launch {
            _uiEvent.emit(HomeUiEvent.NavigateToPreview(normalized))
        }
    }

    fun setHowItWorksVisible(visible: Boolean) {
        _uiState.update { it.copy(showHowItWorksDialog = visible) }
    }

    fun dismissOnboarding() {
        preferencesManager.setOnboardingCompleted(true)
        _uiState.update { it.copy(showOnboardingDialog = false) }
    }
}
