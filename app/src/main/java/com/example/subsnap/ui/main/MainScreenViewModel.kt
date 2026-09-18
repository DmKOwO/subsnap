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
import com.example.subsnap.data.SpacedRepetition
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

enum class CardFilterType(val title: String) {
    ALL("Все"),
    DUE("К повторению"),
    FAVORITES("⭐ Избранное"),
    NEW("Новые"),
    MASTERED("🏆 Усвоено"),
    CEFR_A("A1-A2"),
    CEFR_B("B1-B2"),
    CEFR_C("C1-C2")
}

enum class CardSortOrder(val title: String) {
    NEWEST("Сначала новые"),
    OLDEST("Сначала старые"),
    ALPHABETICAL("По слову (A-Z)"),
    DUE_DATE("По сроку повторения")
}

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
    val smartDetectionEnabled: StateFlow<Boolean> = settingsRepository.smartDetectionEnabled

    val studyDailyGoal: StateFlow<Int> = settingsRepository.studyDailyGoal
    val studyStreakDays: StateFlow<Int> = settingsRepository.studyStreakDays
    val autoPlayTts: StateFlow<Boolean> = settingsRepository.autoPlayTts
    val clozeStudyMode: StateFlow<Boolean> = settingsRepository.clozeStudyMode

    private val _releasesHistory = MutableStateFlow<List<AppReleaseRecord>>(emptyList())
    val releasesHistory: StateFlow<List<AppReleaseRecord>> = _releasesHistory.asStateFlow()

    private val _versionDiff = MutableStateFlow<VersionDiff?>(null)
    val versionDiff: StateFlow<VersionDiff?> = _versionDiff.asStateFlow()

    private val _isLoadingHistory = MutableFlowFalse()
    private fun MutableFlowFalse() = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _cardSearchQuery = MutableStateFlow("")
    val cardSearchQuery: StateFlow<String> = _cardSearchQuery.asStateFlow()

    private val _cardFilter = MutableStateFlow(CardFilterType.ALL)
    val cardFilter: StateFlow<CardFilterType> = _cardFilter.asStateFlow()

    private val _cardSortOrder = MutableStateFlow(CardSortOrder.NEWEST)
    val cardSortOrder: StateFlow<CardSortOrder> = _cardSortOrder.asStateFlow()

    val filteredCards: StateFlow<List<AnkiCard>> = combine(
        cards,
        _cardSearchQuery,
        _cardFilter,
        _cardSortOrder
    ) { list, query, filter, sort ->
        var res = list

        // 1. Text Search
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            res = res.filter { card ->
                card.targetWord.lowercase().contains(q) ||
                card.wordTranslation.lowercase().contains(q) ||
                card.sentence.lowercase().contains(q) ||
                card.sentenceTranslation.lowercase().contains(q) ||
                card.explanation.lowercase().contains(q) ||
                card.userNotes.lowercase().contains(q) ||
                card.tags.any { it.lowercase().contains(q) } ||
                card.partOfSpeech.lowercase().contains(q)
            }
        }

        // 2. Filter
        res = when (filter) {
            CardFilterType.ALL -> res
            CardFilterType.DUE -> res.filter { it.isDue }
            CardFilterType.FAVORITES -> res.filter { it.isFavorite }
            CardFilterType.NEW -> res.filter { it.isNew }
            CardFilterType.MASTERED -> res.filter { it.isMastered }
            CardFilterType.CEFR_A -> res.filter { it.cefrLevel in listOf("A1", "A2") }
            CardFilterType.CEFR_B -> res.filter { it.cefrLevel in listOf("B1", "B2") }
            CardFilterType.CEFR_C -> res.filter { it.cefrLevel in listOf("C1", "C2") }
        }

        // 3. Sort
        when (sort) {
            CardSortOrder.NEWEST -> res.sortedByDescending { it.timestamp }
            CardSortOrder.OLDEST -> res.sortedBy { it.timestamp }
            CardSortOrder.ALPHABETICAL -> res.sortedBy { it.targetWord.lowercase() }
            CardSortOrder.DUE_DATE -> res.sortedWith(
                compareBy<AnkiCard> { !it.isDue }
                    .thenBy { it.nextReviewTimestamp }
            )
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

    val dueCardsCount: StateFlow<Int> = cards.map { list ->
        list.count { it.isDue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val masteredCardsCount: StateFlow<Int> = cards.map { list ->
        list.count { it.isMastered }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favoriteCardsCount: StateFlow<Int> = cards.map { list ->
        list.count { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val retentionRatePercent: StateFlow<Int> = cards.map { list ->
        val reviewed = list.filter { !it.isNew }
        if (reviewed.isEmpty()) {
            100
        } else {
            val goodOrEasy = reviewed.count { it.easeFactor >= 2.4f && it.intervalDays > 1 }
            ((goodOrEasy.toFloat() / reviewed.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

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

    fun toggleCardFavorite(id: String) {
        viewModelScope.launch {
            ankiCardStorage.toggleFavorite(id)
        }
    }

    fun setCardFilter(filter: CardFilterType) {
        _cardFilter.value = filter
    }

    fun setCardSortOrder(sort: CardSortOrder) {
        _cardSortOrder.value = sort
    }

    fun triggerCaptureNow() {
        ScreenCaptureService.triggerCapture(getApplication())
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

    fun isWordDuplicate(targetWord: String, currentCardId: String? = null): Boolean {
        val word = targetWord.trim()
        if (word.isBlank()) return false
        return cards.value.any { it.id != currentCardId && it.targetWord.equals(word, ignoreCase = true) }
    }

    fun resetFiltersAndSearch() {
        _cardSearchQuery.value = ""
        _cardFilter.value = CardFilterType.ALL
    }

    fun exportDeck(filteredOnly: Boolean = false, onExported: (File) -> Unit) {
        viewModelScope.launch {
            val list = if (filteredOnly) filteredCards.value else null
            val file = ankiCardStorage.exportToAnkiFile(list)
            onExported(file)
        }
    }

    fun exportBackupJson(onExported: (File) -> Unit) {
        viewModelScope.launch {
            val file = ankiCardStorage.exportBackupJson()
            onExported(file)
        }
    }

    fun importBackupJson(jsonStr: String, onFinished: (Int) -> Unit) {
        viewModelScope.launch {
            val count = ankiCardStorage.importBackupJson(jsonStr)
            onFinished(count)
        }
    }

    fun recordStudySessionComplete() {
        settingsRepository.recordStudySessionComplete()
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

    fun setSmartDetectionEnabled(enabled: Boolean) {
        settingsRepository.setSmartDetectionEnabled(enabled)
    }

    fun setStudyDailyGoal(goal: Int) {
        settingsRepository.setStudyDailyGoal(goal)
    }

    fun setAutoPlayTts(enabled: Boolean) {
        settingsRepository.setAutoPlayTts(enabled)
    }

    fun setClozeStudyMode(enabled: Boolean) {
        settingsRepository.setClozeStudyMode(enabled)
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
