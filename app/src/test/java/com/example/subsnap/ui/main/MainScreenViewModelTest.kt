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
    }
}
