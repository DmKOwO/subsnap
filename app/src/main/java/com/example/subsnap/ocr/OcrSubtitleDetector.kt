package com.example.subsnap.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

data class SubtitleDetectionResult(
    val hasSubtitles: Boolean,
    val detectedText: String,
    val englishWordCount: Int,
    val subtitleCandidates: List<String> = emptyList()
)

class OcrSubtitleDetector private constructor() {

    @Volatile
    private var recognizer: com.google.mlkit.vision.text.TextRecognizer? = null

    private fun getRecognizer(): com.google.mlkit.vision.text.TextRecognizer {
        return recognizer ?: synchronized(this) {
            recognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also {
                recognizer = it
            }
        }
    }

    suspend fun detectSubtitles(bitmap: Bitmap, region: String = "LOWER_THIRD"): SubtitleDetectionResult = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        processImage(inputImage, bitmap.width, bitmap.height, region)
    }

    suspend fun detectSubtitles(imageFile: File, region: String = "LOWER_THIRD"): SubtitleDetectionResult = withContext(Dispatchers.IO) {
        if (!imageFile.exists()) {
            return@withContext SubtitleDetectionResult(
                hasSubtitles = false,
                detectedText = "",
                englishWordCount = 0
            )
        }
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        if (bitmap == null) {
            return@withContext SubtitleDetectionResult(
                hasSubtitles = false,
                detectedText = "",
                englishWordCount = 0
            )
        }
        try {
            detectSubtitles(bitmap, region)
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun detectSubtitles(context: Context, uri: Uri, region: String = "LOWER_THIRD"): SubtitleDetectionResult = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            processImage(inputImage, inputImage.width, inputImage.height, region)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load InputImage from uri", e)
            SubtitleDetectionResult(hasSubtitles = false, detectedText = "", englishWordCount = 0)
        }
    }

    private suspend fun processImage(
        inputImage: InputImage,
        imageWidth: Int,
        imageHeight: Int,
        region: String = "LOWER_THIRD"
    ): SubtitleDetectionResult = suspendCancellableCoroutine { continuation ->
        getRecognizer().process(inputImage)
            .addOnSuccessListener { visionText ->
                val analysis = analyzeVisionText(visionText, imageWidth, imageHeight, region)
                if (continuation.isActive) {
                    continuation.resume(analysis)
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "OCR recognition failed", error)
                if (continuation.isActive) {
                    // Fail gracefully by assuming text might be present so we don't accidentally drop valid frames
                    continuation.resume(
                        SubtitleDetectionResult(
                            hasSubtitles = true,
                            detectedText = "",
                            englishWordCount = 0
                        )
                    )
                }
            }
    }

    /**
     * Heuristics for subtitle detection:
     * 1. Subtitles typically appear in the lower part of the screen.
     *    - "LOWER_THIRD": checks y > height * 0.55 (or y > height * 0.40 for multi-line).
     *    - "FULL_SCREEN": checks anywhere in the frame.
     * 2. They consist of readable English words (letters [a-zA-Z], length >= 2).
     * 3. Filter out UI watermarks, timestamps (e.g. 12:45), battery stats, or single random letters.
     */
    fun analyzeVisionText(visionText: Text, width: Int, height: Int, region: String = "LOWER_THIRD"): SubtitleDetectionResult {
        val fullRawText = visionText.text.trim()
        if (fullRawText.isBlank()) {
            return SubtitleDetectionResult(
                hasSubtitles = false,
                detectedText = "",
                englishWordCount = 0
            )
        }

        val candidateLines = mutableListOf<String>()
        var totalEnglishWords = 0

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text.trim()
                val lineBox = line.boundingBox

                val inTargetZone = when (region) {
                    "FULL_SCREEN" -> true
                    else -> {
                        if (lineBox != null && height > 0) {
                            lineBox.centerY() > (height * 0.45f)
                        } else true
                    }
                }

                val englishWordsInLine = extractEnglishWords(lineText)

                if (englishWordsInLine.isNotEmpty()) {
                    totalEnglishWords += englishWordsInLine.size
                    if (inTargetZone || englishWordsInLine.size >= 4) {
                        candidateLines.add(lineText)
                    }
                }
            }
        }

        // Subtitles require at least 2 English words and at least 1 candidate line
        val hasSubtitles = totalEnglishWords >= 2 && candidateLines.isNotEmpty()
        val combinedText = candidateLines.joinToString("\n")

        return SubtitleDetectionResult(
            hasSubtitles = hasSubtitles,
            detectedText = if (combinedText.isNotBlank()) combinedText else fullRawText,
            englishWordCount = totalEnglishWords,
            subtitleCandidates = candidateLines
        )
    }

    fun extractEnglishWords(text: String): List<String> {
        val englishWordRegex = Regex("^[a-zA-Z'’-]{2,}$")
        return text.split(Regex("\\s+")).mapNotNull { word ->
            val clean = word.trim('.', ',', '!', '?', '"', ':', ';', '(', ')', '[', ']', '{', '}', '<', '>')
            if (englishWordRegex.matches(clean)) clean else null
        }
    }

    fun isDuplicate(previousText: String, newText: String): Boolean {
        val words1 = extractEnglishWords(previousText).map { it.lowercase() }.toSet()
        val words2 = extractEnglishWords(newText).map { it.lowercase() }.toSet()

        if (words1.isEmpty() || words2.isEmpty()) return false
        if (words1 == words2) return true

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        val jaccard = intersection.toDouble() / union.toDouble()
        return jaccard >= 0.75
    }

    fun isEnglishSubtitleText(text: String): Boolean {
        val englishWords = extractEnglishWords(text)
        return englishWords.size >= 2
    }

    fun close() {
        synchronized(this) {
            try {
                recognizer?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing TextRecognizer", e)
            } finally {
                recognizer = null
            }
        }
    }

    companion object {
        private const val TAG = "OcrSubtitleDetector"

        @Volatile
        private var instance: OcrSubtitleDetector? = null

        fun getInstance(): OcrSubtitleDetector {
            return instance ?: synchronized(this) {
                instance ?: OcrSubtitleDetector().also { instance = it }
            }
        }
    }
}
