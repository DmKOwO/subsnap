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

                val tagsList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (j in 0 until tagsArr.length()) {
                        val t = tagsArr.optString(j, "").trim()
                        if (t.isNotBlank()) tagsList.add(t)
                    }
                }

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
                        lastReviewedTimestamp = obj.optLong("lastReviewedTimestamp", 0L),
                        partOfSpeech = obj.optString("partOfSpeech", ""),
                        cefrLevel = obj.optString("cefrLevel", ""),
                        clozeSentence = obj.optString("clozeSentence", ""),
                        tags = tagsList,
                        isFavorite = obj.optBoolean("isFavorite", false),
                        userNotes = obj.optString("userNotes", "")
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
        val existingIndex = current.indexOfFirst { it.id == card.id }
        if (existingIndex >= 0) {
            current[existingIndex] = card
        } else {
            current.add(0, card)
        }
        persist(current)
    }

    suspend fun toggleFavorite(id: String): Unit = withContext(Dispatchers.IO) {
        val current = _cards.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val old = current[idx]
            current[idx] = old.copy(isFavorite = !old.isFavorite)
            persist(current)
        }
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
                put("partOfSpeech", card.partOfSpeech)
                put("cefrLevel", card.cefrLevel)
                put("clozeSentence", card.clozeSentence)
                put("tags", JSONArray(card.tags))
                put("isFavorite", card.isFavorite)
                put("userNotes", card.userNotes)
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
                writer.write(card.toAnkiTsvRow() + "\n")
            }
        }
        exportFile
    }

    /**
     * Exports a full JSON backup of the deck including all metadata and learning progress.
     */
    suspend fun exportBackupJson(): File = withContext(Dispatchers.IO) {
        val backupFile = File(exportsDir, "subsnap_deck_backup_${System.currentTimeMillis()}.json")
        val content = if (cardsFile.exists()) cardsFile.readText() else "[]"
        backupFile.writeText(content)
        backupFile
    }

    /**
     * Restores/merges cards from a JSON backup. Returns count of imported cards.
     */
    suspend fun importBackupJson(jsonStr: String): Int = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray(jsonStr)
            val currentMap = _cards.value.associateBy { it.id }.toMutableMap()
            var addedCount = 0

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "imported_${UUID.randomUUID().toString().take(8)}")
                val imgPath = obj.optString("imagePath", "")
                val imgFile = File(imgPath)

                val tagsList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (j in 0 until tagsArr.length()) {
                        val t = tagsArr.optString(j, "").trim()
                        if (t.isNotBlank()) tagsList.add(t)
                    }
                }

                val card = AnkiCard(
                    id = id,
                    screenshotFile = imgFile,
                    targetWord = obj.optString("targetWord", ""),
                    transcription = obj.optString("transcription", ""),
                    wordTranslation = obj.optString("wordTranslation", ""),
                    sentence = obj.optString("sentence", ""),
                    sentenceTranslation = obj.optString("sentenceTranslation", ""),
                    explanation = obj.optString("explanation", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    repetitions = obj.optInt("repetitions", 0),
                    intervalDays = obj.optInt("intervalDays", 0),
                    easeFactor = obj.optDouble("easeFactor", 2.5).toFloat(),
                    nextReviewTimestamp = obj.optLong("nextReviewTimestamp", 0L),
                    lastReviewedTimestamp = obj.optLong("lastReviewedTimestamp", 0L),
                    partOfSpeech = obj.optString("partOfSpeech", ""),
                    cefrLevel = obj.optString("cefrLevel", ""),
                    clozeSentence = obj.optString("clozeSentence", ""),
                    tags = tagsList,
                    isFavorite = obj.optBoolean("isFavorite", false),
                    userNotes = obj.optString("userNotes", "")
                )

                if (card.targetWord.isNotBlank() && card.sentence.isNotBlank()) {
                    currentMap[id] = card
                    addedCount++
                }
            }

            persist(currentMap.values.sortedByDescending { it.timestamp })
            addedCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
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
