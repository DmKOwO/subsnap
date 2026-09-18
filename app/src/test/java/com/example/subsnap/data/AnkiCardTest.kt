package com.example.subsnap.data

import com.example.subsnap.data.model.AnkiCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnkiCardTest {

    @Test
    fun toAnkiTsvRow_formatsValidThreeColumnTsvWithImageTag() {
        val card = AnkiCard(
            id = "card_123",
            screenshotFile = File("/storage/emulated/0/snap_456.webp"),
            targetWord = "serendipity",
            transcription = "[ˌsɛrənˈdɪpɪti]",
            wordTranslation = "счастливая случайность",
            sentence = "It was pure serendipity that we met.",
            sentenceTranslation = "Это была чистая случайность, что мы встретились.",
            explanation = "Находка чего-то приятного без намеренного поиска.",
            cefrLevel = "C1",
            tags = listOf("idioms", "advanced"),
            timestamp = 1600000000000L
        )

        val tsv3 = card.toAnkiTsvRow(includeTagsColumn = true)
        val parts3 = tsv3.split("\t")

        assertEquals(3, parts3.size)
        val front = parts3[0]
        val back = parts3[1]
        val tags = parts3[2]

        assertTrue(front.contains("It was pure serendipity that we met."))
        assertTrue(front.contains("<img src=\"snap_456.webp\">"))
        assertTrue(back.contains("<b>serendipity</b>"))
        assertTrue(back.contains("<i>[ˌsɛrənˈdɪpɪti]</i>"))
        assertTrue(back.contains("счастливая случайность"))
        assertTrue(back.contains("Это была чистая случайность"))
        assertTrue(tags.contains("idioms"))
        assertTrue(tags.contains("advanced"))
        assertTrue(tags.contains("cefr_c1"))
        assertTrue(tags.contains("subsnap"))

        val tsv2 = card.toAnkiTsvRow(includeTagsColumn = false)
        val parts2 = tsv2.split("\t")
        assertEquals(2, parts2.size)
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
        assertEquals(3, columns.size)

        val tsv2 = card.toAnkiTsvRow(includeTagsColumn = false)
        assertEquals(2, tsv2.split("\t").size)
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
    fun computedClozeSentence_doesNotCorruptSubstringsInsideUnrelatedWords() {
        // "cat" is inside "Education", but it must NOT be replaced!
        val card = AnkiCard(
            id = "cloze_boundary_1",
            screenshotFile = File("snap.webp"),
            targetWord = "cat",
            transcription = "",
            wordTranslation = "кот",
            sentence = "Education is essential for everyone.",
            sentenceTranslation = "",
            explanation = ""
        )
        assertEquals("Education is essential for everyone.", card.computedClozeSentence)

        // But standalone "cat" is replaced
        val standaloneCatCard = card.copy(sentence = "The cat is sleeping on the couch.")
        assertEquals("The [...] is sleeping on the couch.", standaloneCatCard.computedClozeSentence)
    }

    @Test
    fun computedClozeSentence_matchesInflectedWordForms() {
        // "run" matches inflected form "running"
        val card = AnkiCard(
            id = "cloze_inflection_1",
            screenshotFile = File("snap.webp"),
            targetWord = "run",
            transcription = "",
            wordTranslation = "бежать",
            sentence = "She was running late for work.",
            sentenceTranslation = "",
            explanation = ""
        )
        assertEquals("She was [...] late for work.", card.computedClozeSentence)
    }

    @Test
    fun cefrHelpers_findWordRange_handlesExactAndInflectedBoundaries() {
        val sentence = "The curious cats were exploring the forest."
        val exactRange = com.example.subsnap.ui.main.CefrHelpers.findWordRange(sentence, "forest")
        org.junit.Assert.assertNotNull(exactRange)
        assertEquals("forest", sentence.substring(exactRange!!))

        val inflectedRange = com.example.subsnap.ui.main.CefrHelpers.findWordRange(sentence, "cat")
        org.junit.Assert.assertNotNull(inflectedRange)
        assertEquals("cats", sentence.substring(inflectedRange!!))

        // Negative test: "lore" inside "exploring" should not match
        val insideRange = com.example.subsnap.ui.main.CefrHelpers.findWordRange(sentence, "lore")
        org.junit.Assert.assertNull(insideRange)
    }

    @Test
    fun cefrHelpers_frequencyTiers_returnsAccurateTiers() {
        assertEquals("Top 1k", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("apple", "A1"))
        assertEquals("Top 3k", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("travel", "A2"))
        assertEquals("Top 5k", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("persuade", "B1"))
        assertEquals("Top 8k", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("elaborate", "B2"))
        assertEquals("Top 15k", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("quintessential", "C1"))
        assertEquals("Rare", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("sesquipedalian", "C2"))

        // Fallback by length when CEFR is empty
        assertEquals("Top 3k", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("dog"))
        assertEquals("Top 5k", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("welcome"))
        assertEquals("Top 8k", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("celebrate"))
        assertEquals("Rare", com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier("incomprehensibility"))
    }
}

