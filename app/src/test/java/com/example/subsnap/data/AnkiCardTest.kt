package com.example.subsnap.data

import com.example.subsnap.data.model.AnkiCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnkiCardTest {

    @Test
    fun toAnkiTsvRow_formatsValidTwoColumnTsvWithImageTag() {
        val card = AnkiCard(
            id = "card_123",
            screenshotFile = File("/storage/emulated/0/snap_456.webp"),
            targetWord = "serendipity",
            transcription = "[ˌsɛrənˈdɪpɪti]",
            wordTranslation = "счастливая случайность",
            sentence = "It was pure serendipity that we met.",
            sentenceTranslation = "Это была чистая случайность, что мы встретились.",
            explanation = "Находка чего-то приятного без намеренного поиска.",
            timestamp = 1600000000000L
        )

        val tsv = card.toAnkiTsvRow()
        val parts = tsv.split("\t")

        assertEquals(2, parts.size)
        val front = parts[0]
        val back = parts[1]

        assertTrue(front.contains("It was pure serendipity that we met."))
        assertTrue(front.contains("<img src=\"snap_456.webp\">"))
        assertTrue(back.contains("<b>serendipity</b>"))
        assertTrue(back.contains("<i>[ˌsɛrənˈdɪpɪti]</i>"))
        assertTrue(back.contains("счастливая случайность"))
        assertTrue(back.contains("Это была чистая случайность"))
    }

    @Test
    fun toAnkiTsvRow_cleansInternalTabsAndNewlines() {
        val card = AnkiCard(
            id = "card_dirty",
            screenshotFile = File("test.webp"),
            targetWord = "clean\tword\n",
            transcription = "[kl\tiːn]\n",
            wordTranslation = "чистый\tперевод\n",
            sentence = "Line 1\nLine 2\twith tabs",
            sentenceTranslation = "Перевод\nс переносом\tи табуляцией",
            explanation = "Объяснение\nстрока 2",
            timestamp = 1000L
        )

        val tsv = card.toAnkiTsvRow()
        val columns = tsv.split("\t")
        assertEquals(2, columns.size)
    }

    @Test
    fun intervalStatusText_displaysAppropriateStatus() {
        val newCard = AnkiCard(
            id = "card_new",
            screenshotFile = File("a.webp"),
            targetWord = "word",
            transcription = "",
            wordTranslation = "слово",
            sentence = "A word.",
            sentenceTranslation = "Слово.",
            explanation = ""
        )
        assertEquals("Новая", newCard.intervalStatusText)

        val oneDay = newCard.copy(repetitions = 1, intervalDays = 1, lastReviewedTimestamp = 1000L)
        assertEquals("1 день", oneDay.intervalStatusText)

        val threeDays = newCard.copy(repetitions = 2, intervalDays = 3, lastReviewedTimestamp = 1000L)
        assertEquals("3 дня", threeDays.intervalStatusText)

        val tenDays = newCard.copy(repetitions = 3, intervalDays = 10, lastReviewedTimestamp = 1000L)
        assertEquals("10 дней", tenDays.intervalStatusText)

        val twentyOneDays = newCard.copy(repetitions = 4, intervalDays = 21, lastReviewedTimestamp = 1000L)
        assertEquals("21 день", twentyOneDays.intervalStatusText)

        val twentyTwoDays = newCard.copy(repetitions = 5, intervalDays = 22, lastReviewedTimestamp = 1000L)
        assertEquals("22 дня", twentyTwoDays.intervalStatusText)

        val twentyFiveDays = newCard.copy(repetitions = 6, intervalDays = 25, lastReviewedTimestamp = 1000L)
        assertEquals("25 дней", twentyFiveDays.intervalStatusText)
    }

    @Test
    fun cleanJsonOutput_stripsMarkdownCodeBlocks() {
        val rawWithFence = "```json\n{\"sentence\": \"Hello world\", \"target_word\": \"world\"}\n```"
        val cleaned = com.example.subsnap.ai.GeminiApiClient.cleanJsonOutput(rawWithFence)
        assertEquals("{\"sentence\": \"Hello world\", \"target_word\": \"world\"}", cleaned)

        val rawWithGenericFence = "```\n{\"sentence\": \"Hello\"}\n```"
        val cleanedGeneric = com.example.subsnap.ai.GeminiApiClient.cleanJsonOutput(rawWithGenericFence)
        assertEquals("{\"sentence\": \"Hello\"}", cleanedGeneric)
    }

    @Test
    fun cleanJsonOutput_handlesPreambleAndPostambleText() {
        val rawWithPreamble = "Here is the parsed card:\n```json\n{\"sentence\": \"Hello world\", \"target_word\": \"world\"}\n```\nHope this helps!"
        val cleaned = com.example.subsnap.ai.GeminiApiClient.cleanJsonOutput(rawWithPreamble)
        assertEquals("{\"sentence\": \"Hello world\", \"target_word\": \"world\"}", cleaned)
    }

    @Test
    fun cleanJsonOutput_handlesRawJsonWithoutFences() {
        val rawPure = "   {\"sentence\": \"Pure JSON\", \"target_word\": \"pure\"}   "
        val cleaned = com.example.subsnap.ai.GeminiApiClient.cleanJsonOutput(rawPure)
        assertEquals("{\"sentence\": \"Pure JSON\", \"target_word\": \"pure\"}", cleaned)
    }

    @Test
    fun computedClozeSentence_replacesTargetWordWithDeletionBlank() {
        val card = AnkiCard(
            id = "cloze_1",
            screenshotFile = File("snap.webp"),
            targetWord = "epiphany",
            transcription = "",
            wordTranslation = "озарение",
            sentence = "She had a sudden epiphany while watching the sunset.",
            sentenceTranslation = "",
            explanation = ""
        )
        assertEquals("She had a sudden [...] while watching the sunset.", card.computedClozeSentence)

        // Custom cloze sentence overrides automatic cloze
        val customClozeCard = card.copy(clozeSentence = "She had an {{c1::epiphany}}.")
        assertEquals("She had an {{c1::epiphany}}.", customClozeCard.computedClozeSentence)
    }

    @Test
    fun isMastered_returnsTrueWhenRepetitionsAndIntervalAreHigh() {
        val card = AnkiCard(
            id = "mastered_1",
            screenshotFile = File("snap.webp"),
            targetWord = "tenacity",
            transcription = "",
            wordTranslation = "упорство",
            sentence = "His tenacity paid off.",
            sentenceTranslation = "",
            explanation = "",
            repetitions = 3,
            intervalDays = 21
        )
        assertTrue(card.isMastered)

        val beginnerCard = card.copy(repetitions = 2, intervalDays = 6)
        org.junit.Assert.assertFalse(beginnerCard.isMastered)
    }

    @Test
    fun cefrHelpers_correctlyIdentifiesLevelsAndLabels() {
        assertEquals("B2 · Выше среднего", com.example.subsnap.ui.main.CefrHelpers.getCefrLabel("B2"))
        assertEquals("A1 · Начальный", com.example.subsnap.ui.main.CefrHelpers.getCefrLabel("A1"))
        assertEquals("C2 · Владение в совершенстве", com.example.subsnap.ui.main.CefrHelpers.getCefrLabel("C2"))
    }
}

