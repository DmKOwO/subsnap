package com.example.subsnap.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.subsnap.ai.GeminiApiClient
import com.example.subsnap.data.AnkiCardStorage
import com.example.subsnap.data.CapturedScreenshot
import com.example.subsnap.data.ScreenshotStorage
import com.example.subsnap.data.SettingsRepository
import com.example.subsnap.data.model.AnkiCard
import com.example.subsnap.service.ScreenCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val screenshotStorage = ScreenshotStorage.getInstance(application)
    private val ankiCardStorage = AnkiCardStorage.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    private val geminiClient = GeminiApiClient.getInstance(application)

    val screenshots: StateFlow<List<CapturedScreenshot>> = screenshotStorage.screenshots
    val cards: StateFlow<List<AnkiCard>> = ankiCardStorage.cards
    val apiKey: StateFlow<String> = settingsRepository.apiKey
    val selectedModel: StateFlow<String> = settingsRepository.selectedModel
    val filterEmptyScreenshots: StateFlow<Boolean> = settingsRepository.filterEmptyScreenshots
    val serviceState: StateFlow<ScreenCaptureService.Companion.ServiceState> =
        ScreenCaptureService.serviceState

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    private val _generatedCard = MutableStateFlow<AnkiCard?>(null)
    val generatedCard: StateFlow<AnkiCard?> = _generatedCard.asStateFlow()

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

    fun analyzeScreenshot(screenshot: CapturedScreenshot) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null
            _generatedCard.value = null

            val result = geminiClient.analyzeScreenshot(screenshot.file)
            result.onSuccess { card ->
                _generatedCard.value = card
            }.onFailure { err ->
                _analysisError.value = err.message ?: "Ошибка при обращении к ИИ"
            }

            _isAnalyzing.value = false
        }
    }

    fun dismissGeneratedCard() {
        _generatedCard.value = null
        _analysisError.value = null
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
}
