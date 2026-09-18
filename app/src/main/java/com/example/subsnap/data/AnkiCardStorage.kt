package com.example.subsnap.data

import android.content.Context
import com.example.subsnap.data.model.AnkiCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AnkiCardStorage(private val context: Context) {

    private val cardsFile: File = File(context.filesDir, "anki_cards.json")
    private val exportsDir: File = File(context.filesDir, "exports").apply {
        if (!exists()) mkdirs()
    }

    private val _cards = MutableStateFlow<List<AnkiCard>>(emptyList())
    val cards: StateFlow<List<AnkiCard>> = _cards.asStateFlow()

    init {
        loadCards()
    }

    fun loadCards() {
        if (!cardsFile.exists()) {
            _cards.value = emptyList()
            return
        }

        try {
            val jsonStr = cardsFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AnkiCard>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val imgPath = obj.getString("imagePath")
                val imgFile = File(imgPath)

                list.add(
                    AnkiCard(
                        id = obj.getString("id"),
                        screenshotFile = imgFile,
                        targetWord = obj.getString("targetWord"),
                        transcription = obj.optString("transcription", ""),
                        wordTranslation = obj.getString("wordTranslation"),
                        sentence = obj.getString("sentence"),
                        sentenceTranslation = obj.getString("sentenceTranslation"),
                        explanation = obj.optString("explanation", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        repetitions = obj.optInt("repetitions", 0),
                        intervalDays = obj.optInt("intervalDays", 0),
                        easeFactor = obj.optDouble("easeFactor", 2.5).toFloat(),
                        nextReviewTimestamp = obj.optLong("nextReviewTimestamp", 0L),
                        lastReviewedTimestamp = obj.optLong("lastReviewedTimestamp", 0L)
                    )
                )
            }
            _cards.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            _cards.value = emptyList()
        }
    }

    suspend fun saveCard(card: AnkiCard): Unit = withContext(Dispatchers.IO) {
        val current = _cards.value.toMutableList()
        current.removeAll { it.id == card.id }
        current.add(0, card)
        persist(current)
    }

    suspend fun deleteCard(id: String): Unit = withContext(Dispatchers.IO) {
        val current = _cards.value.toMutableList()
        current.removeAll { it.id == id }
        persist(current)
    }

    suspend fun clearAllCards(): Unit = withContext(Dispatchers.IO) {
        persist(emptyList())
    }

    private fun persist(list: List<AnkiCard>) {
        val jsonArray = JSONArray()
        for (card in list) {
            val obj = JSONObject().apply {
                put("id", card.id)
                put("imagePath", card.screenshotFile.absolutePath)
                put("targetWord", card.targetWord)
                put("transcription", card.transcription)
                put("wordTranslation", card.wordTranslation)
                put("sentence", card.sentence)
                put("sentenceTranslation", card.sentenceTranslation)
                put("explanation", card.explanation)
                put("timestamp", card.timestamp)
                put("repetitions", card.repetitions)
                put("intervalDays", card.intervalDays)
                put("easeFactor", card.easeFactor.toDouble())
                put("nextReviewTimestamp", card.nextReviewTimestamp)
                put("lastReviewedTimestamp", card.lastReviewedTimestamp)
            }
            jsonArray.put(obj)
        }
        cardsFile.writeText(jsonArray.toString(2))
        _cards.value = list
    }

    /**
     * Exports all cards to a standard Anki TSV file ready to import in Anki / AnkiDroid.
     */
    suspend fun exportToAnkiFile(): File = withContext(Dispatchers.IO) {
        val exportFile = File(exportsDir, "subsnap_anki_deck_${System.currentTimeMillis()}.txt")
        FileOutputStream(exportFile).bufferedWriter().use { writer ->
            writer.write("#separator:tab\n")
            writer.write("#html:true\n")
            writer.write("#tags column:3\n")
            for (card in _cards.value) {
                writer.write(card.toAnkiTsvRow() + "\tSubSnap\n")
            }
        }
        exportFile
    }

    companion object {
        @Volatile
        private var instance: AnkiCardStorage? = null

        fun getInstance(context: Context): AnkiCardStorage {
            return instance ?: synchronized(this) {
                instance ?: AnkiCardStorage(context.applicationContext).also { instance = it }
            }
        }
    }
}
