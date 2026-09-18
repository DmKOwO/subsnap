package com.example.subsnap.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrSubtitleDetectorTest {

    private val detector = OcrSubtitleDetector.getInstance()

    @Test
    fun extractEnglishWords_extractsValidWordsAndCleansPunctuation() {
        val input = "Hello, world! It's a brand-new day... (really) [100%]"
        val words = detector.extractEnglishWords(input)

        assertTrue(words.contains("Hello"))
        assertTrue(words.contains("world"))
        assertTrue(words.contains("It's"))
        assertTrue(words.contains("brand-new"))
        assertTrue(words.contains("day"))
        assertTrue(words.contains("really"))
        assertFalse(words.contains("100%"))
    }

    @Test
    fun isEnglishSubtitleText_identifiesRealSubtitles() {
        assertTrue(detector.isEnglishSubtitleText("We need to leave right now."))
        assertTrue(detector.isEnglishSubtitleText("What are you doing here?"))
        assertTrue(detector.isEnglishSubtitleText("Don't worry about it."))
    }

    @Test
    fun isEnglishSubtitleText_filtersEmptyOrGarbage() {
        assertFalse(detector.isEnglishSubtitleText(""))
        assertFalse(detector.isEnglishSubtitleText("   "))
        assertFalse(detector.isEnglishSubtitleText("12:45:00"))
        assertFalse(detector.isEnglishSubtitleText("4G LTE 100%"))
        assertFalse(detector.isEnglishSubtitleText("... --- ..."))
    }

    @Test
    fun isEnglishSubtitleText_filtersCyrillicOnly() {
        assertFalse(detector.isEnglishSubtitleText("Привет мир, как дела?"))
        assertFalse(detector.isEnglishSubtitleText("Загрузка видео..."))
    }

    @Test
    fun isEnglishSubtitleText_requiresAtLeastTwoWords() {
        assertFalse(detector.isEnglishSubtitleText("OK"))
        assertFalse(detector.isEnglishSubtitleText("Netflix"))
        assertTrue(detector.isEnglishSubtitleText("Let's go"))
    }

    @Test
    fun close_canBeCalledSafelyAndRepeatedly() {
        detector.close()
        detector.close() // Should not throw
        assertTrue(detector.isEnglishSubtitleText("Let's go ahead"))
    }

    @Test
    fun detectSubtitles_nonExistentFile_returnsFalseGracefully() = kotlinx.coroutines.test.runTest {
        val nonExistent = java.io.File("/tmp/does_not_exist_${System.currentTimeMillis()}.webp")
        val result = detector.detectSubtitles(nonExistent)
        assertFalse(result.hasSubtitles)
        assertEquals(0, result.englishWordCount)
        assertEquals("", result.detectedText)
    }

    @Test
    fun isDuplicate_detectsIdenticalOrMinorVariations() {
        val line1 = "We have to run away right now!"
        val line2 = "We have to run away right now."
        assertTrue(detector.isDuplicate(line1, line2))

        val line3 = "We have to run away right now immediately!"
        assertTrue(detector.isDuplicate(line1, line3))

        val different = "Where did everybody go today?"
        assertFalse(detector.isDuplicate(line1, different))
    }

    @Test
    fun filterCandidateLines_lowerThird_ignoresTopHeadersAndTitles() {
        val screenHeight = 1000
        val topTitle = OcrSubtitleDetector.RecognizedLine("Video Player: The Lost Temple (English CC)", centerY = 50)
        val bottomSubtitle = OcrSubtitleDetector.RecognizedLine("I think we found the secret passage!", centerY = 850)

        val (candidateLines, wordCount) = detector.filterCandidateLines(
            lines = listOf(topTitle, bottomSubtitle),
            height = screenHeight,
            region = "LOWER_THIRD"
        )

        assertEquals(1, candidateLines.size)
        assertEquals("I think we found the secret passage!", candidateLines[0])
        assertEquals(6, wordCount)
    }

    @Test
    fun filterCandidateLines_lowerThird_emptyWhenOnlyTopHeadersPresent() {
        val screenHeight = 1000
        val topTitle = OcrSubtitleDetector.RecognizedLine("Video Player: The Lost Temple (English CC)", centerY = 50)
        val addressBar = OcrSubtitleDetector.RecognizedLine("chrome https google com search", centerY = 120)

        val (candidateLines, wordCount) = detector.filterCandidateLines(
            lines = listOf(topTitle, addressBar),
            height = screenHeight,
            region = "LOWER_THIRD"
        )

        assertTrue(candidateLines.isEmpty())
        assertEquals(0, wordCount)
    }

    @Test
    fun filterCandidateLines_fullScreen_includesTopAndBottom() {
        val screenHeight = 1000
        val topTitle = OcrSubtitleDetector.RecognizedLine("Video Player: The Lost Temple (English CC)", centerY = 50)
        val bottomSubtitle = OcrSubtitleDetector.RecognizedLine("I think we found the secret passage!", centerY = 850)

        val (candidateLines, wordCount) = detector.filterCandidateLines(
            lines = listOf(topTitle, bottomSubtitle),
            height = screenHeight,
            region = "FULL_SCREEN"
        )

        assertEquals(2, candidateLines.size)
        assertEquals(13, wordCount)
    }
}
