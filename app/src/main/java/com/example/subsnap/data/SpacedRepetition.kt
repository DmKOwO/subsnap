package com.example.subsnap.data

import com.example.subsnap.data.model.AnkiCard
import kotlin.math.max
import kotlin.math.roundToInt

enum class ReviewRating {
    AGAIN,
    HARD,
    GOOD,
    EASY
}

object SpacedRepetition {

    private const val ONE_MINUTE_MS = 60_000L
    private const val ONE_DAY_MS = 86_400_000L
    const val MIN_EASE_FACTOR = 1.3f
    const val DEFAULT_EASE_FACTOR = 2.5f

    /**
     * Calculates the updated AnkiCard after a user reviews it with a given rating.
     * Uses the SM-2 algorithm customized for mobile flashcards.
     */
    fun calculateNextReview(
        card: AnkiCard,
        rating: ReviewRating,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): AnkiCard {
        var repetitions = card.repetitions
        var easeFactor = card.easeFactor
        val intervalDays: Int
        val nextReviewTimestamp: Long

        when (rating) {
            ReviewRating.AGAIN -> {
                repetitions = 0
                intervalDays = 0
                easeFactor = max(MIN_EASE_FACTOR, easeFactor - 0.2f)
                // Due again in 10 minutes
                nextReviewTimestamp = currentTimeMillis + (10 * ONE_MINUTE_MS)
            }
            ReviewRating.HARD -> {
                easeFactor = max(MIN_EASE_FACTOR, easeFactor - 0.15f)
                repetitions += 1
                intervalDays = if (card.intervalDays <= 0) 1 else max(1, (card.intervalDays * 1.2f).toInt())
                nextReviewTimestamp = currentTimeMillis + (intervalDays * ONE_DAY_MS)
            }
            ReviewRating.GOOD -> {
                repetitions += 1
                intervalDays = when (repetitions) {
                    1 -> 1
                    2 -> 6
                    else -> max(1, (card.intervalDays * easeFactor).roundToInt())
                }
                nextReviewTimestamp = currentTimeMillis + (intervalDays * ONE_DAY_MS)
            }
            ReviewRating.EASY -> {
                repetitions += 1
                easeFactor += 0.15f
                intervalDays = when (repetitions) {
                    1 -> 4
                    else -> max(2, (card.intervalDays * easeFactor * 1.3f).roundToInt())
                }
                nextReviewTimestamp = currentTimeMillis + (intervalDays * ONE_DAY_MS)
            }
        }

        return card.copy(
            repetitions = repetitions,
            intervalDays = intervalDays,
            easeFactor = easeFactor,
            nextReviewTimestamp = nextReviewTimestamp,
            lastReviewedTimestamp = currentTimeMillis
        )
    }

    /**
     * Formats interval preview for rating buttons (e.g. "<10 мин", "1 дн", "6 дн").
     */
    fun formatIntervalPreview(card: AnkiCard, rating: ReviewRating): String {
        return when (rating) {
            ReviewRating.AGAIN -> "<10 мин"
            ReviewRating.HARD -> {
                val days = if (card.intervalDays <= 0) 1 else max(1, (card.intervalDays * 1.2f).toInt())
                "$days дн"
            }
            ReviewRating.GOOD -> {
                val nextReps = card.repetitions + 1
                val days = when (nextReps) {
                    1 -> 1
                    2 -> 6
                    else -> max(1, (card.intervalDays * card.easeFactor).roundToInt())
                }
                "$days дн"
            }
            ReviewRating.EASY -> {
                val nextReps = card.repetitions + 1
                val days = when (nextReps) {
                    1 -> 4
                    else -> max(2, (card.intervalDays * card.easeFactor * 1.3f).roundToInt())
                }
                "$days дн"
            }
        }
    }

    fun isDue(card: AnkiCard, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        // If nextReviewTimestamp is 0 or <= now, the card is ready for study
        return card.nextReviewTimestamp <= currentTimeMillis
    }

    fun getDueCards(cards: List<AnkiCard>, currentTimeMillis: Long = System.currentTimeMillis()): List<AnkiCard> {
        return cards.filter { isDue(it, currentTimeMillis) }
    }

    /**
     * Formats card count according to Russian pluralization rules:
     * 1 карточка, 2 карточки, 5 карточек, 11 карточек, 21 карточка, etc.
     */
    fun formatCardsCount(count: Int): String {
        val rem100 = count % 100
        val rem10 = count % 10
        return when {
            rem100 in 11..19 -> "$count карточек"
            rem10 == 1 -> "$count карточка"
            rem10 in 2..4 -> "$count карточки"
            else -> "$count карточек"
        }
    }
}
