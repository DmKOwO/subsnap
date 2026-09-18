package com.example.subsnap.ui.main

import com.example.subsnap.data.CapturedScreenshot
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.io.File

class MainScreenViewModelTest {
    @Test
    fun capturedScreenshot_formattedDate_isNotEmpty() {
        val screenshot = CapturedScreenshot(
            id = "test_1",
            file = File("/tmp/test.webp"),
            timestamp = 1700000000000L,
            width = 1080,
            height = 1920,
            sizeBytes = 1024
        )
        assertEquals(true, screenshot.formattedDate.isNotEmpty())
        assertEquals(1080, screenshot.width)
        assertEquals(1920, screenshot.height)
        assertEquals("test_1", screenshot.id)
    }

    @Test
    fun settingsRepository_availableModels_containsActiveGeminiModels() {
        val models = com.example.subsnap.data.SettingsRepository.AVAILABLE_MODELS
        assertEquals(true, models.contains("gemini-3.8-flash"))
        assertEquals(true, models.contains("gemini-3.5-flash"))
        assertEquals(true, models.contains("gemini-3.1-flash-lite"))
    }

    @Test
    fun settingsRepository_defaultAutoCaptureInterval_isReasonableForSubtitles() {
        val interval = com.example.subsnap.data.SettingsRepository.DEFAULT_AUTO_CAPTURE_INTERVAL
        assertEquals(true, interval in 1.5f..4.0f)
    }
}
