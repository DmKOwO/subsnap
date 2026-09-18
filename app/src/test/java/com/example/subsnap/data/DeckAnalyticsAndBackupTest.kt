package com.example.subsnap.data

import com.example.subsnap.data.model.AnkiCard
import com.example.subsnap.ui.main.CardFilterType
import com.example.subsnap.ui.main.CardSortOrder
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeckAnalyticsAndBackupTest {

    private fun createSampleCard(
        id: String,
        targetWord: String,
        cefrLevel: String = "",
        isFavorite: Boolean = false,
        repetitions: Int = 0,
        intervalDays: Int = 0,
        nextReviewTimestamp: Long = 0L,
        timestamp: Long = 1000L
    ): AnkiCard {
        return AnkiCard(
            id = id,
            screenshotFile = File("/tmp/$id.webp"),
            targetWord = targetWord,
            transcription = "",
            wordTranslation = "перевод $targetWord",
            sentence = "Example with $targetWord.",
            sentenceTranslation = "Пример с $targetWord.",
            explanation = "Объяснение",
            timestamp = timestamp,
            cefrLevel = cefrLevel,
            isFavorite = isFavorite,
            repetitions = repetitions,
            intervalDays = intervalDays,
            nextReviewTimestamp = nextReviewTimestamp
        )
    }

    @Test
    fun filterCards_correctlyFiltersByFilterType() {
        val now = 100_000L
        val cardDue = createSampleCard("c1", "apple", cefrLevel = "A1", nextReviewTimestamp = now - 500L)
        val cardMastered = createSampleCard("c2", "eloquent", cefrLevel = "C1", repetitions = 4, intervalDays = 30)
        val cardFav = createSampleCard("c3", "serendipity", cefrLevel = "B2", isFavorite = true)
        val cardFuture = createSampleCard("c4", "dog", cefrLevel = "A2", nextReviewTimestamp = now + 10_000L)

        val all = listOf(cardDue, cardMastered, cardFav, cardFuture)

        // ALL
        assertEquals(4, all.size)

        // FAVORITE
        val favs = all.filter { it.isFavorite }
        assertEquals(1, favs.size)
        assertEquals("serendipity", favs.first().targetWord)

        // MASTERED
        val mastered = all.filter { it.isMastered }
        assertEquals(1, mastered.size)
        assertEquals("eloquent", mastered.first().targetWord)

        // CEFR_A
        val cefrA = all.filter { it.cefrLevel.startsWith("A", ignoreCase = true) }
        assertEquals(2, cefrA.size)

        // CEFR_B
        val cefrB = all.filter { it.cefrLevel.startsWith("B", ignoreCase = true) }
        assertEquals(1, cefrB.size)
        assertEquals("serendipity", cefrB.first().targetWord)

        // CEFR_C
        val cefrC = all.filter { it.cefrLevel.startsWith("C", ignoreCase = true) }
        assertEquals(1, cefrC.size)
        assertEquals("eloquent", cefrC.first().targetWord)
    }

    @Test
    fun sortCards_correctlySortsByOrder() {
        val c1 = createSampleCard("1", "banana", timestamp = 100L, nextReviewTimestamp = 500L)
        val c2 = createSampleCard("2", "apple", timestamp = 300L, nextReviewTimestamp = 100L)
        val c3 = createSampleCard("3", "cherry", timestamp = 200L, nextReviewTimestamp = 800L)
        val list = listOf(c1, c2, c3)

        // NEWEST (timestamp desc)
        val newest = list.sortedByDescending { it.timestamp }
        assertEquals(listOf("apple", "cherry", "banana"), newest.map { it.targetWord })

        // OLDEST (timestamp asc)
        val oldest = list.sortedBy { it.timestamp }
        assertEquals(listOf("banana", "cherry", "apple"), oldest.map { it.targetWord })

        // TARGET_WORD (alphabetical)
        val alphabetical = list.sortedBy { it.targetWord.lowercase() }
        assertEquals(listOf("apple", "banana", "cherry"), alphabetical.map { it.targetWord })

        // NEXT_REVIEW (review asc)
        val nextReview = list.sortedBy { it.nextReviewTimestamp }
        assertEquals(listOf("apple", "banana", "cherry"), nextReview.map { it.targetWord })
    }

    @Test
    fun jsonBackupFormat_serializesAndParsesAllFields() {
        val card = AnkiCard(
            id = "test_full_card",
            screenshotFile = File("/storage/test.webp"),
            targetWord = "lucid",
            transcription = "[ˈluːsɪd]",
            wordTranslation = "ясный",
            sentence = "She had a lucid dream.",
            sentenceTranslation = "У неё был осознанный сон.",
            explanation = "Четкий, понятный.",
            timestamp = 1700000000000L,
            repetitions = 2,
            intervalDays = 6,
            easeFactor = 2.45f,
            nextReviewTimestamp = 1700500000000L,
            lastReviewedTimestamp = 1700000000000L,
            partOfSpeech = "adjective",
            cefrLevel = "B2",
            clozeSentence = "She had a [...] dream.",
            tags = listOf("C1-Prep", "Sleep"),
            isFavorite = true,
            userNotes = "Important exam vocabulary"
        )

        val json = JSONObject()
        json.put("id", card.id)
        json.put("screenshotPath", card.screenshotFile.absolutePath)
        json.put("targetWord", card.targetWord)
        json.put("transcription", card.transcription)
        json.put("wordTranslation", card.wordTranslation)
        json.put("sentence", card.sentence)
        json.put("sentenceTranslation", card.sentenceTranslation)
        json.put("explanation", card.explanation)
        json.put("timestamp", card.timestamp)
        json.put("repetitions", card.repetitions)
        json.put("intervalDays", card.intervalDays)
        json.put("easeFactor", card.easeFactor.toDouble())
        json.put("nextReviewTimestamp", card.nextReviewTimestamp)
        json.put("lastReviewedTimestamp", card.lastReviewedTimestamp)
        json.put("partOfSpeech", card.partOfSpeech)
        json.put("cefrLevel", card.cefrLevel)
        json.put("clozeSentence", card.clozeSentence)
        val tagsArr = JSONArray()
        card.tags.forEach { tagsArr.put(it) }
        json.put("tags", tagsArr)
        json.put("isFavorite", card.isFavorite)
        json.put("userNotes", card.userNotes)

        // Read back
        assertEquals("test_full_card", json.getString("id"))
        assertEquals("lucid", json.getString("targetWord"))
        assertEquals("adjective", json.getString("partOfSpeech"))
        assertEquals("B2", json.getString("cefrLevel"))
        assertEquals("She had a [...] dream.", json.getString("clozeSentence"))
        assertTrue(json.getBoolean("isFavorite"))
        assertEquals("Important exam vocabulary", json.getString("userNotes"))
        val readTags = mutableListOf<String>()
        val tagsJsonArr = json.getJSONArray("tags")
        for (i in 0 until tagsJsonArr.length()) {
            readTags.add(tagsJsonArr.getString(i))
        }
        assertEquals(listOf("C1-Prep", "Sleep"), readTags)
    }
}
