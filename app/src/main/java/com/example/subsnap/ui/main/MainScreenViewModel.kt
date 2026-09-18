package com.example.subsnap.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.subsnap.BuildConfig
import com.example.subsnap.ai.GeminiApiClient
import com.example.subsnap.data.AnkiCardStorage
import com.example.subsnap.data.CapturedScreenshot
import com.example.subsnap.data.ScreenshotStorage
import com.example.subsnap.data.SettingsRepository
import com.example.subsnap.data.model.AnkiCard
import com.example.subsnap.ocr.OcrSubtitleDetector
import com.example.subsnap.ota.AppReleaseRecord
import com.example.subsnap.ota.VersionDiff
import com.example.subsnap.service.ScreenCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val screenshotStorage = ScreenshotStorage.getInstance(application)
    private val ankiCardStorage = AnkiCardStorage.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    private val geminiClient = GeminiApiClient.getInstance(application)
    private val ocrDetector = OcrSubtitleDetector.getInstance()
    private val updateManager = com.example.subsnap.ota.GitHubUpdateManager.getInstance(application)

    val screenshots: StateFlow<List<CapturedScreenshot>> = screenshotStorage.screenshots
    val cards: StateFlow<List<AnkiCard>> = ankiCardStorage.cards
    val apiKey: StateFlow<String> = settingsRepository.apiKey
    val selectedModel: StateFlow<String> = settingsRepository.selectedModel
    val filterEmptyScreenshots: StateFlow<Boolean> = settingsRepository.filterEmptyScreenshots
    val githubRepo: StateFlow<String> = settingsRepository.githubRepo
    val serviceState: StateFlow<ScreenCaptureService.Companion.ServiceState> =
        ScreenCaptureService.serviceState

    val downloadState: StateFlow<com.example.subsnap.ota.GitHubUpdateManager.DownloadState> =
        updateManager.downloadState

    val ttsSpeed: StateFlow<Float> = settingsRepository.ttsSpeed
    val ttsLocale: StateFlow<String> = settingsRepository.ttsLocale
    val ocrRegion: StateFlow<String> = settingsRepository.ocrRegion
    val skipDuplicateSubtitles: StateFlow<Boolean> = settingsRepository.skipDuplicateSubtitles
    val autoCaptureIntervalSec: StateFlow<Float> = settingsRepository.autoCaptureIntervalSec
    val autoStartAutoCapture: StateFlow<Boolean> = settingsRepository.autoStartAutoCapture

    private val _releasesHistory = MutableStateFlow<List<AppReleaseRecord>>(emptyList())
    val releasesHistory: StateFlow<List<AppReleaseRecord>> = _releasesHistory.asStateFlow()

    private val _versionDiff = MutableStateFlow<VersionDiff?>(null)
    val versionDiff: StateFlow<VersionDiff?> = _versionDiff.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _cardSearchQuery = MutableStateFlow("")
    val cardSearchQuery: StateFlow<String> = _cardSearchQuery.asStateFlow()

    val filteredCards: StateFlow<List<AnkiCard>> = combine(cards, _cardSearchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.trim().lowercase()
            list.filter { card ->
                card.targetWord.lowercase().contains(q) ||
                card.wordTranslation.lowercase().contains(q) ||
                card.sentence.lowercase().contains(q) ||
                card.sentenceTranslation.lowercase().contains(q) ||
                card.explanation.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCardsCount: StateFlow<Int> = cards.map { list ->
        val startOfDay = getStartOfDayMillis()
        list.count { it.timestamp >= startOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayScreenshotsCount: StateFlow<Int> = screenshots.map { list ->
        val startOfDay = getStartOfDayMillis()
        list.count { it.timestamp >= startOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _updateInfo = MutableStateFlow<com.example.subsnap.ota.AppUpdateInfo?>(null)
    val updateInfo: StateFlow<com.example.subsnap.ota.AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    private val _lastFailedScreenshot = MutableStateFlow<CapturedScreenshot?>(null)
    val lastFailedScreenshot: StateFlow<CapturedScreenshot?> = _lastFailedScreenshot.asStateFlow()

    private val _generatedCard = MutableStateFlow<AnkiCard?>(null)
    val generatedCard: StateFlow<AnkiCard?> = _generatedCard.asStateFlow()

    private fun getStartOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun refreshScreenshots() {
        screenshotStorage.refresh()
    }

    fun deleteScreenshot(id: String) {
        viewModelScope.launch {
            screenshotStorage.deleteScreenshot(id)
        }
    }

    fun clearAllScreenshots() {
        viewModelScope.launch {
            screenshotStorage.clearAll()
        }
    }

    fun setApiKey(key: String) {
        settingsRepository.setApiKey(key)
    }

    fun setSelectedModel(model: String) {
        settingsRepository.setSelectedModel(model)
    }

    fun setFilterEmptyScreenshots(enabled: Boolean) {
        settingsRepository.setFilterEmptyScreenshots(enabled)
    }

    fun updateCard(card: AnkiCard) {
        viewModelScope.launch {
            ankiCardStorage.saveCard(card)
        }
    }

    fun analyzeScreenshot(screenshot: CapturedScreenshot, forceSend: Boolean = false) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null
            _lastFailedScreenshot.value = screenshot
            _generatedCard.value = null

            // 1. On-device ML Kit OCR subtitle detection before sending frame to cloud
            val ocrResult = ocrDetector.detectSubtitles(screenshot.file)
            if (!forceSend && !ocrResult.hasSubtitles && settingsRepository.filterEmptyScreenshots.value) {
                _isAnalyzing.value = false
                _analysisError.value = "ML Kit OCR: на кадре не обнаружены английские субтитры или речь. Нажмите «Всё равно отправить», если субтитры на кадре есть, или выберите другой кадр."
                return@launch
            }

            val result = geminiClient.analyzeScreenshot(
                screenshotFile = screenshot.file,
                ocrHint = ocrResult.detectedText.ifBlank { null }
            )
            result.onSuccess { card ->
                _generatedCard.value = card
                _lastFailedScreenshot.value = null
            }.onFailure { err ->
                _analysisError.value = err.message ?: "Ошибка при обращении к ИИ"
            }

            _isAnalyzing.value = false
        }
    }

    fun dismissGeneratedCard() {
        _generatedCard.value = null
        _analysisError.value = null
        _lastFailedScreenshot.value = null
    }

    fun saveGeneratedCard(card: AnkiCard) {
        viewModelScope.launch {
            ankiCardStorage.saveCard(card)
            _generatedCard.value = null
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            ankiCardStorage.deleteCard(id)
        }
    }

    fun clearAllCards() {
        viewModelScope.launch {
            ankiCardStorage.clearAllCards()
        }
    }

    fun exportDeck(onExported: (File) -> Unit) {
        viewModelScope.launch {
            val file = ankiCardStorage.exportToAnkiFile()
            onExported(file)
        }
    }

    fun checkForUpdates(userInitiated: Boolean = false) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateMessage.value = null
            val result = updateManager.checkForUpdates(settingsRepository.githubRepo.value)
            result.onSuccess { info ->
                if (info.isUpdateAvailable) {
                    _updateInfo.value = info
                } else if (userInitiated) {
                    _updateMessage.value = "У вас установлена самая свежая версия (${info.currentVersion})"
                }
            }.onFailure { err ->
                if (userInitiated) {
                    _updateMessage.value = err.message ?: "Не удалось проверить обновления"
                }
            }
            _isCheckingUpdate.value = false
        }
    }

    fun downloadAndInstallUpdate(downloadUrl: String) {
        viewModelScope.launch {
            val result = updateManager.downloadApk(downloadUrl)
            result.onSuccess { apkFile ->
                updateManager.installApk(apkFile)
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
        _updateMessage.value = null
        updateManager.resetState()
    }

    fun setCardSearchQuery(query: String) {
        _cardSearchQuery.value = query
    }

    fun loadReleaseHistory() {
        viewModelScope.launch {
            _isLoadingHistory.value = true
            val result = updateManager.fetchAllReleases(settingsRepository.githubRepo.value)
            result.onSuccess { list ->
                _releasesHistory.value = list
                val diff = updateManager.calculateVersionDiff(BuildConfig.VERSION_NAME, list)
                _versionDiff.value = diff
            }.onFailure { err ->
                _updateMessage.value = "Ошибка загрузки истории релизов: ${err.message}"
            }
            _isLoadingHistory.value = false
        }
    }

    fun setTtsSpeed(speed: Float) {
        settingsRepository.setTtsSpeed(speed)
    }

    fun setTtsLocale(locale: String) {
        settingsRepository.setTtsLocale(locale)
    }

    fun setOcrRegion(region: String) {
        settingsRepository.setOcrRegion(region)
    }

    fun setSkipDuplicateSubtitles(skip: Boolean) {
        settingsRepository.setSkipDuplicateSubtitles(skip)
    }

    fun setAutoCaptureIntervalSec(interval: Float) {
        settingsRepository.setAutoCaptureIntervalSec(interval)
    }

    fun setAutoStartAutoCapture(enabled: Boolean) {
        settingsRepository.setAutoStartAutoCapture(enabled)
    }

    fun toggleAutoCapture() {
        ScreenCaptureService.toggleAuto(getApplication())
    }

    fun setAutoCapture(enabled: Boolean) {
        ScreenCaptureService.setAuto(getApplication(), enabled)
    }

    fun setGithubRepo(repo: String) {
        settingsRepository.setGithubRepo(repo)
    }
}
