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
    val lastReviewedTimestamp: Long = 0L,
    val partOfSpeech: String = "",
    val cefrLevel: String = "",
    val clozeSentence: String = "",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val userNotes: String = ""
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

    val isMastered: Boolean
        get() = intervalDays >= 21 || repetitions >= 4

    val isLearning: Boolean
        get() = !isNew && !isMastered

    val computedClozeSentence: String
        get() {
            if (clozeSentence.isNotBlank()) return clozeSentence
            val range = com.example.subsnap.ui.main.CefrHelpers.findWordRange(sentence, targetWord)
            return if (range != null) {
                sentence.substring(0, range.first) + "[...]" + sentence.substring(range.last + 1)
            } else {
                sentence
            }
        }

    val frequencyTier: String
        get() = com.example.subsnap.ui.main.CefrHelpers.getFrequencyTier(targetWord, cefrLevel)

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
     * Front: Sentence (with cloze option) + bold target word + image
     * Back: Word translation, transcription, sentence translation, context explanation, CEFR & tags
     * Tags Column: Space-separated tags compatible with Anki '#tags column:3' header directive.
     */
    fun toAnkiTsvRow(includeTagsColumn: Boolean = true): String {
        val cleanSentence = sentence.replace("\t", " ").replace("\n", " ")
        val cleanSentenceTrans = sentenceTranslation.replace("\t", " ").replace("\n", " ")
        val cleanExplanation = explanation.replace("\t", " ").replace("\n", " ")
        val cleanTargetWord = targetWord.replace("\t", " ").replace("\n", " ")
        val cleanTranscription = transcription.replace("\t", " ").replace("\n", " ")
        val cleanWordTranslation = wordTranslation.replace("\t", " ").replace("\n", " ")
        val cleanNotes = userNotes.replace("\t", " ").replace("\n", " ")
        val imgTag = "<img src=\"${screenshotFile.name}\">"

        val posBadge = if (partOfSpeech.isNotBlank()) " <small>[$partOfSpeech]</small>" else ""
        val cefrBadge = if (cefrLevel.isNotBlank()) " <small>[$cefrLevel]</small>" else ""
        val notesPart = if (cleanNotes.isNotBlank()) "<br><br><b>Заметки:</b> $cleanNotes" else ""

        val rawTags = (tags + (if (cefrLevel.isNotBlank()) listOf("cefr_${cefrLevel.lowercase()}") else emptyList()) + listOf("subsnap"))
        val cleanTags = rawTags.map { it.replace(" ", "_").replace("\t", "").replace("\n", "") }.distinct().joinToString(" ")
        val tagsPart = if (cleanTags.isNotBlank()) "<br><br><small>Теги: $cleanTags</small>" else ""

        val front = "$cleanSentence<br><br>$imgTag"
        val back = "<b>$cleanTargetWord</b>$posBadge$cefrBadge <i>$cleanTranscription</i><br><b>Перевод:</b> $cleanWordTranslation<br><br><b>Предложение:</b> $cleanSentenceTrans<br><b>Контекст:</b> $cleanExplanation$notesPart$tagsPart"

        return if (includeTagsColumn) "$front\t$back\t$cleanTags" else "$front\t$back"
    }
}
