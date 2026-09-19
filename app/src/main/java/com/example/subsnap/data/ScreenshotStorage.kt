package com.example.subsnap.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CapturedScreenshot(
    val id: String,
    val file: File,
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

class ScreenshotStorage(private val context: Context) {

    private val screenshotsDir: File = File(context.filesDir, "screenshots").apply {
        if (!exists()) mkdirs()
    }

    private val _screenshots = MutableStateFlow<List<CapturedScreenshot>>(emptyList())
    val screenshots: StateFlow<List<CapturedScreenshot>> = _screenshots.asStateFlow()

    init {
        refresh()
    }

    private var lastTrashedItem: Pair<CapturedScreenshot, File>? = null

    fun refresh() {
        val files = screenshotsDir.listFiles { f ->
            f.extension.lowercase() in listOf("webp", "jpg", "png") && !f.name.startsWith(".trash_")
        } ?: emptyArray()

        val list = files.map { file ->
            CapturedScreenshot(
                id = file.nameWithoutExtension,
                file = file,
                timestamp = file.lastModified(),
                width = 0,
                height = 0,
                sizeBytes = file.length()
            )
        }.sortedByDescending { it.timestamp }

        _screenshots.value = list
    }

    suspend fun saveScreenshot(bitmap: Bitmap): CapturedScreenshot = withContext(Dispatchers.IO) {
        val id = "snap_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val file = File(screenshotsDir, "$id.webp")

        FileOutputStream(file).use { out ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
            } else {
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
            }
        }

        val item = CapturedScreenshot(
            id = id,
            file = file,
            timestamp = file.lastModified(),
            width = bitmap.width,
            height = bitmap.height,
            sizeBytes = file.length()
        )

        refresh()
        item
    }

    suspend fun deleteScreenshot(id: String): CapturedScreenshot? = withContext(Dispatchers.IO) {
        // Purge previous trashed file if any
        lastTrashedItem?.second?.delete()
        lastTrashedItem = null

        val targetItem = _screenshots.value.find { it.id == id } ?: return@withContext null
        val trashFile = File(screenshotsDir, ".trash_${targetItem.file.name}")
        val renamed = targetItem.file.renameTo(trashFile)
        if (renamed) {
            lastTrashedItem = targetItem to trashFile
            refresh()
            targetItem
        } else {
            targetItem.file.delete()
            refresh()
            targetItem
        }
    }

    suspend fun undoDelete(): Boolean = withContext(Dispatchers.IO) {
        val trashed = lastTrashedItem ?: return@withContext false
        val originalFile = trashed.first.file
        val restored = trashed.second.renameTo(originalFile)
        lastTrashedItem = null
        if (restored) {
            refresh()
        }
        restored
    }

    suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        lastTrashedItem?.second?.delete()
        lastTrashedItem = null
        screenshotsDir.listFiles()?.forEach { it.delete() }
        refresh()
    }

    companion object {
        @Volatile
        private var instance: ScreenshotStorage? = null

        fun getInstance(context: Context): ScreenshotStorage {
            return instance ?: synchronized(this) {
                instance ?: ScreenshotStorage(context.applicationContext).also { instance = it }
            }
        }
    }
}
