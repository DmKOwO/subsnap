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

    @Test
    fun batchGenerationState_initial_isNotRunningAndZeroProgress() {
        val state = BatchGenerationState()
        assertEquals(false, state.isRunning)
        assertEquals(0, state.current)
        assertEquals(0, state.total)
        assertEquals(0.0f, state.progress)
        assertEquals(false, state.isCancelled)
        assertEquals(false, state.isCompleted)
    }

    @Test
    fun batchGenerationState_calculatesProgressCorrectly() {
        val state = BatchGenerationState(
            isRunning = true,
            current = 3,
            total = 10,
            successCount = 2,
            skippedCount = 1,
            errorCount = 0
        )
        assertEquals(true, state.isRunning)
        assertEquals(0.3f, state.progress)
        assertEquals(2, state.successCount)
        assertEquals(1, state.skippedCount)
    }

    @Test
    fun batchGenerationState_zeroTotal_doesNotProduceNan() {
        val state = BatchGenerationState(total = 0, current = 0)
        assertEquals(0.0f, state.progress)
        assertEquals(false, state.progress.isNaN())
    }

    @Test
    fun batchGenerationState_completionState_hasExpectedFlags() {
        val completed = BatchGenerationState(
            isRunning = false,
            current = 5,
            total = 5,
            isCompleted = true,
            successCount = 5
        )
        assertEquals(false, completed.isRunning)
        assertEquals(true, completed.isCompleted)
        assertEquals(1.0f, completed.progress)
        assertEquals(5, completed.successCount)
    }

    @Test
    fun batchGenerationState_cancelledState_hasExpectedFlags() {
        val cancelled = BatchGenerationState(
            isRunning = false,
            current = 2,
            total = 10,
            isCancelled = true,
            successCount = 1
        )
        assertEquals(false, cancelled.isRunning)
        assertEquals(true, cancelled.isCancelled)
        assertEquals(0.2f, cancelled.progress)
    }
}
