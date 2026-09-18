package com.example.subsnap.ai

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.subsnap.data.SettingsRepository
import com.example.subsnap.data.model.AnkiCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class GeminiApiClient(private val context: Context) {

    private val settingsRepository = SettingsRepository.getInstance(context)

    suspend fun analyzeScreenshot(screenshotFile: File): Result<AnkiCard> = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.apiKey.value
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("API-ключ Gemini не настроен. Перейдите в Настройки и укажите бесплатный ключ.")
            )
        }

        if (!screenshotFile.exists()) {
            return@withContext Result.failure(
                IllegalArgumentException("Файл скриншота не найден: ${screenshotFile.absolutePath}")
            )
        }

        val model = settingsRepository.selectedModel.value
        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            // Read image and encode to Base64
            val imageBytes = screenshotFile.readBytes()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val mimeType = if (screenshotFile.extension.lowercase() == "webp") "image/webp" else "image/jpeg"

            // Construct payload
            val promptText = """
                You are an expert English language teacher helping a student memorize vocabulary using Anki.
                Analyze this screenshot taken while watching a movie/video, reading, or gaming.
                1. Locate the English subtitles, dialogue, or visible speech text.
                2. Extract the complete English sentence.
                3. Choose the most useful or challenging English vocabulary word or idiom from the sentence.
                4. Provide the IPA phonetic transcription for that target word.
                5. Provide a natural Russian translation for the target word.
                6. Provide a natural Russian translation of the full sentence.
                7. Provide a concise Russian explanation of how the word works in this context.

                Return ONLY a JSON object matching this schema:
                {
                  "sentence": "...",
                  "target_word": "...",
                  "transcription": "[...]",
                  "word_translation": "...",
                  "sentence_translation": "...",
                  "explanation": "..."
                }
                If absolutely no English text or subtitles are found in the image, return:
                {
                  "sentence": "No subtitles found",
                  "target_word": "None",
                  "transcription": "",
                  "word_translation": "Текст субтитров не обнаружен",
                  "sentence_translation": "На кадре нет субтитров",
                  "explanation": "Попробуйте захватить другой момент видео"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            // Text prompt
                            put(JSONObject().apply { put("text", promptText) })
                            // Image data
                            put(JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mime_type", mimeType)
                                    put("data", base64Image)
                                }
                                put("inline_data", inlineData)
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.2)
                }
                put("generationConfig", generationConfig)
            }

            // HTTP POST
            val url = URL(endpointUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 30000
                readTimeout = 30000
                doOutput = true
                doInput = true
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseText = if (responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            } else {
                val errorStream = conn.errorStream ?: conn.inputStream
                val errorBody = BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                Log.e(TAG, "Gemini API error ($responseCode): $errorBody")

                val msg = when (responseCode) {
                    400 -> "Неверный запрос или формат изображения (400)"
                    403 -> "Неверный API-ключ Gemini или доступ ограничен (403)"
                    429 -> "Превышен лимит запросов Gemini (подождите минуту) (429)"
                    500, 503 -> "Сервер Gemini временно недоступен (50x)"
                    else -> "Ошибка Gemini API: код $responseCode"
                }
                return@withContext Result.failure(Exception(msg))
            }

            // Parse response
            val responseObj = JSONObject(responseText)
            val candidates = responseObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("Модель не вернула ответ"))
            }

            val parts = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
            val rawJsonOutput = parts.getJSONObject(0).getString("text")

            val cardJson = JSONObject(rawJsonOutput)
            val card = AnkiCard(
                id = "card_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                screenshotFile = screenshotFile,
                sentence = cardJson.optString("sentence", "").trim(),
                targetWord = cardJson.optString("target_word", "").trim(),
                transcription = cardJson.optString("transcription", "").trim(),
                wordTranslation = cardJson.optString("word_translation", "").trim(),
                sentenceTranslation = cardJson.optString("sentence_translation", "").trim(),
                explanation = cardJson.optString("explanation", "").trim(),
                timestamp = System.currentTimeMillis()
            )

            Result.success(card)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze screenshot with Gemini", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "GeminiApiClient"

        @Volatile
        private var instance: GeminiApiClient? = null

        fun getInstance(context: Context): GeminiApiClient {
            return instance ?: synchronized(this) {
                instance ?: GeminiApiClient(context.applicationContext).also { instance = it }
            }
        }
    }
}
