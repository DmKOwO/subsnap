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
            targetWord = "clean",
            transcription = "",
            wordTranslation = "чистый",
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
    }
}
