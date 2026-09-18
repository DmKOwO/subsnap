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
}
