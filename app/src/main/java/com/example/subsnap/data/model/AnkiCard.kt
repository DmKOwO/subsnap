package com.example.subsnap.data.model

import com.example.subsnap.data.SpacedRepetition
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AnkiCard(
    val id: String,
    val screenshotFile: File,
    val targetWord: String,
    val transcription: String,
    val wordTranslation: String,
    val sentence: String,
    val sentenceTranslation: String,
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Float = SpacedRepetition.DEFAULT_EASE_FACTOR,
    val nextReviewTimestamp: Long = 0L,
    val lastReviewedTimestamp: Long = 0L
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("HH:mm dd.MM.yy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val isDue: Boolean
        get() = SpacedRepetition.isDue(this)

    val isNew: Boolean
        get() = repetitions == 0 && lastReviewedTimestamp == 0L

    val intervalStatusText: String
        get() = when {
            isNew -> "Новая"
            intervalDays == 0 -> "Снова"
            else -> {
                val rem100 = intervalDays % 100
                val rem10 = intervalDays % 10
                when {
                    rem100 in 11..19 -> "$intervalDays дней"
                    rem10 == 1 -> "$intervalDays день"
                    rem10 in 2..4 -> "$intervalDays дня"
                    else -> "$intervalDays дней"
                }
            }
        }

    /**
     * Generates a TSV row compatible with Anki import.
     * Front: Sentence with bold target word + image
     * Back: Word translation, transcription, sentence translation, explanation
     */
    fun toAnkiTsvRow(): String {
        val cleanSentence = sentence.replace("\t", " ").replace("\n", " ")
        val cleanSentenceTrans = sentenceTranslation.replace("\t", " ").replace("\n", " ")
        val cleanExplanation = explanation.replace("\t", " ").replace("\n", " ")
        val imgTag = "<img src=\"${screenshotFile.name}\">"

        val front = "$cleanSentence<br><br>$imgTag"
        val back = "<b>$targetWord</b> <i>$transcription</i><br><b>Перевод:</b> $wordTranslation<br><br><b>Предложение:</b> $cleanSentenceTrans<br><b>Контекст:</b> $cleanExplanation"

        return "$front\t$back"
    }
}
