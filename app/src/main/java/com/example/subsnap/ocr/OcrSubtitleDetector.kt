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

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun detectSubtitles(bitmap: Bitmap): SubtitleDetectionResult = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        processImage(inputImage, bitmap.width, bitmap.height)
    }

    suspend fun detectSubtitles(imageFile: File): SubtitleDetectionResult = withContext(Dispatchers.IO) {
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
            detectSubtitles(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun detectSubtitles(context: Context, uri: Uri): SubtitleDetectionResult = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            processImage(inputImage, inputImage.width, inputImage.height)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load InputImage from uri", e)
            SubtitleDetectionResult(hasSubtitles = false, detectedText = "", englishWordCount = 0)
        }
    }

    private suspend fun processImage(
        inputImage: InputImage,
        imageWidth: Int,
        imageHeight: Int
    ): SubtitleDetectionResult = suspendCancellableCoroutine { continuation ->
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val analysis = analyzeVisionText(visionText, imageWidth, imageHeight)
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
     * 1. Subtitles typically appear in the lower half of the screen (y > height * 0.35).
     * 2. They consist of readable English words (letters [a-zA-Z], length >= 2).
     * 3. Filter out UI watermarks, timestamps (e.g. 12:45), battery stats, or single random letters.
     */
    fun analyzeVisionText(visionText: Text, width: Int, height: Int): SubtitleDetectionResult {
        val fullRawText = visionText.text.trim()
        if (fullRawText.isBlank()) {
            return SubtitleDetectionResult(
                hasSubtitles = false,
                detectedText = "",
                englishWordCount = 0
            )
        }

        val englishWordRegex = Regex("^[a-zA-Z'’-]{2,}$")
        val candidateLines = mutableListOf<String>()
        var totalEnglishWords = 0

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text.trim()
                val lineBox = line.boundingBox

                // Subtitles are most commonly in the lower 65% of the frame
                val isLowerOrCenter = if (lineBox != null && height > 0) {
                    lineBox.centerY() > (height * 0.35f)
                } else true

                val englishWordsInLine = extractEnglishWords(lineText)

                if (englishWordsInLine.isNotEmpty()) {
                    totalEnglishWords += englishWordsInLine.size
                    if (isLowerOrCenter || englishWordsInLine.size >= 3) {
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

    fun isEnglishSubtitleText(text: String): Boolean {
        val englishWords = extractEnglishWords(text)
        return englishWords.size >= 2
    }

    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TextRecognizer", e)
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
