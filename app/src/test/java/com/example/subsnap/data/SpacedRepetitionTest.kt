package com.example.subsnap.data

import com.example.subsnap.data.model.AnkiCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpacedRepetitionTest {

    private val baseCard = AnkiCard(
        id = "test_card_1",
        screenshotFile = File("/tmp/snap.webp"),
        targetWord = "resilience",
        transcription = "[rɪˈzɪliəns]",
        wordTranslation = "устойчивость",
        sentence = "His resilience helped him succeed.",
        sentenceTranslation = "Его устойчивость помогла ему преуспеть.",
        explanation = "Способность быстро восстанавливаться после трудностей.",
        timestamp = 1000000L
    )

    @Test
    fun calculateNextReview_again_resetsRepetitionsAndDueInTenMinutes() {
        val reviewedCard = baseCard.copy(repetitions = 3, intervalDays = 15, easeFactor = 2.5f)
        val now = 10_000_000L
        val next = SpacedRepetition.calculateNextReview(reviewedCard, ReviewRating.AGAIN, now)

        assertEquals(0, next.repetitions)
        assertEquals(0, next.intervalDays)
        assertEquals(2.3f, next.easeFactor, 0.001f)
        assertEquals(now + 10 * 60_000L, next.nextReviewTimestamp)
        assertEquals(now, next.lastReviewedTimestamp)
    }

    @Test
    fun calculateNextReview_good_progressesCorrectly() {
        val now = 10_000_000L
        // Step 1: from 0 reps
        val rep1 = SpacedRepetition.calculateNextReview(baseCard, ReviewRating.GOOD, now)
        assertEquals(1, rep1.repetitions)
        assertEquals(1, rep1.intervalDays)
        assertEquals(now + 1 * 86_400_000L, rep1.nextReviewTimestamp)

        // Step 2: from 1 rep
        val rep2 = SpacedRepetition.calculateNextReview(rep1, ReviewRating.GOOD, now)
        assertEquals(2, rep2.repetitions)
        assertEquals(6, rep2.intervalDays)
        assertEquals(now + 6 * 86_400_000L, rep2.nextReviewTimestamp)

        // Step 3: from 2 reps with EF=2.5
        val rep3 = SpacedRepetition.calculateNextReview(rep2, ReviewRating.GOOD, now)
        assertEquals(3, rep3.repetitions)
        assertEquals(15, rep3.intervalDays) // 6 * 2.5 = 15
    }

    @Test
    fun calculateNextReview_easy_boostsEaseFactorAndGivesHigherInitialInterval() {
        val now = 10_000_000L
        val rep1 = SpacedRepetition.calculateNextReview(baseCard, ReviewRating.EASY, now)
        assertEquals(1, rep1.repetitions)
        assertEquals(4, rep1.intervalDays)
        assertEquals(2.65f, rep1.easeFactor, 0.001f)
        assertEquals(now + 4 * 86_400_000L, rep1.nextReviewTimestamp)
    }

    @Test
    fun calculateNextReview_hard_decreasesEaseFactorWithoutResettingReps() {
        val card = baseCard.copy(repetitions = 2, intervalDays = 6, easeFactor = 2.5f)
        val now = 10_000_000L
        val next = SpacedRepetition.calculateNextReview(card, ReviewRating.HARD, now)

        assertEquals(3, next.repetitions)
        assertEquals(7, next.intervalDays) // 6 * 1.2 = 7.2 -> 7
        assertEquals(2.35f, next.easeFactor, 0.001f)
    }

    @Test
    fun calculateNextReview_easeFactorNeverDropsBelowMinimum() {
        var card = baseCard.copy(easeFactor = 1.35f)
        card = SpacedRepetition.calculateNextReview(card, ReviewRating.AGAIN)
        assertEquals(SpacedRepetition.MIN_EASE_FACTOR, card.easeFactor, 0.001f)
    }

    @Test
    fun isDue_correctlyFlagsDueCards() {
        val now = 5_000_000L
        val dueCard = baseCard.copy(nextReviewTimestamp = now - 1000L)
        val futureCard = baseCard.copy(nextReviewTimestamp = now + 100000L)
        val newCard = baseCard.copy(nextReviewTimestamp = 0L)

        assertTrue(SpacedRepetition.isDue(dueCard, now))
        assertTrue(SpacedRepetition.isDue(newCard, now))
        assertFalse(SpacedRepetition.isDue(futureCard, now))

        val dueList = SpacedRepetition.getDueCards(listOf(dueCard, futureCard, newCard), now)
        assertEquals(2, dueList.size)
        assertEquals(listOf(dueCard, newCard), dueList)
    }

    @Test
    fun formatIntervalPreview_formatsProperHumanStrings() {
        val newCard = baseCard
        assertEquals("<10 мин", SpacedRepetition.formatIntervalPreview(newCard, ReviewRating.AGAIN))
        assertEquals("1 дн", SpacedRepetition.formatIntervalPreview(newCard, ReviewRating.HARD))
        assertEquals("1 дн", SpacedRepetition.formatIntervalPreview(newCard, ReviewRating.GOOD))
        assertEquals("4 дн", SpacedRepetition.formatIntervalPreview(newCard, ReviewRating.EASY))
    }
}
