package com.example.subsnap.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.res.Configuration
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.subsnap.MainActivity
import com.example.subsnap.data.ScreenshotStorage
import com.example.subsnap.data.SettingsRepository
import com.example.subsnap.ocr.OcrSubtitleDetector
import com.example.subsnap.ocr.SubtitleBandDiffDetector
import com.example.subsnap.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.random.Random

class ScreenCaptureService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoCaptureJob: Job? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private lateinit var screenshotStorage: ScreenshotStorage
    private lateinit var settingsRepository: SettingsRepository
    private val ocrDetector = OcrSubtitleDetector.getInstance()
    private val diffDetector = SubtitleBandDiffDetector()
    private var overlayManager: OverlayManager? = null
    private var displayManager: DisplayManager? = null

    private var screenWidth = 1080
    private var screenHeight = 1920
    private var screenDensity = 420

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                checkAndUpdateOrientation()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        screenshotStorage = ScreenshotStorage.getInstance(this)
        settingsRepository = SettingsRepository.getInstance(this)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager?.registerDisplayListener(displayListener, null)

        startBackgroundThread()
        initDisplayMetrics()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    startAsForeground()
                    initMediaProjection(resultCode, resultData)
                    setupOverlay()
                    _serviceState.update { it.copy(isRunning = true, isAutoCapture = false) }
                    val shouldAutoStart = intent.getBooleanExtra(
                        EXTRA_START_AUTO,
                        settingsRepository.autoStartAutoCapture.value
                    )
                    if (shouldAutoStart) {
                        startAutoCapture()
                    }
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_TRIGGER_CAPTURE -> {
                captureAndSave()
            }
            ACTION_TOGGLE_AUTO -> {
                toggleAutoCapture()
            }
            ACTION_START_AUTO -> {
                startAutoCapture()
            }
            ACTION_STOP_AUTO -> {
                stopAutoCapture()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification(
            title = "SubSnap: Захват экрана активен",
            content = "Нажмите на плавающий виджет для создания снимка"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(content: String) {
        val notification = buildNotification(
            title = "SubSnap: Захват экрана активен",
            content = content
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun initDisplayMetrics() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            screenWidth = metrics.bounds.width()
            screenHeight = metrics.bounds.height()
            screenDensity = resources.configuration.densityDpi
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
        }
        Log.d(TAG, "Screen metrics initialized: ${screenWidth}x${screenHeight} @ ${screenDensity}dpi")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged: orientation=${newConfig.orientation}")
        checkAndUpdateOrientation()
    }

    private fun checkAndUpdateOrientation() {
        val prevWidth = screenWidth
        val prevHeight = screenHeight
        initDisplayMetrics()

        if (prevWidth != screenWidth || prevHeight != screenHeight) {
            Log.d(TAG, "Display orientation changed from ${prevWidth}x${prevHeight} to ${screenWidth}x${screenHeight}. Resizing VirtualDisplay...")
            serviceScope.launch(Dispatchers.Main) {
                updateVirtualDisplaySize()
            }
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("ScreenCaptureBackground").apply {
            start()
            backgroundHandler = Handler(looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
        backgroundThread = null
        backgroundHandler = null
    }

    private fun initMediaProjection(resultCode: Int, resultData: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.w(TAG, "MediaProjection stopped by system")
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@ScreenCaptureService,
                        "Захват экрана остановлен системой (выбирайте «Весь экран» при запросе)",
                        Toast.LENGTH_LONG
                    ).show()
                }
                cleanupProjection()
                stopSelf()
            }
        }, backgroundHandler)

        createVirtualDisplay()
    }

    private fun createVirtualDisplay() {
        if (mediaProjection == null) return

        imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "SubSnapVirtualDisplay",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            backgroundHandler
        )
    }

    private fun updateVirtualDisplaySize() {
        val vd = virtualDisplay
        if (vd == null) {
            createVirtualDisplay()
            return
        }

        try {
            val oldReader = imageReader
            val newReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )
            imageReader = newReader

            // Dynamic resize in-place without invalidating MediaProjection token!
            vd.resize(screenWidth, screenHeight, screenDensity)
            vd.surface = newReader.surface

            oldReader?.close()
            diffDetector.reset()
            Log.d(TAG, "VirtualDisplay successfully resized to ${screenWidth}x${screenHeight} @ ${screenDensity}dpi")
        } catch (e: Exception) {
            Log.e(TAG, "Error resizing VirtualDisplay on orientation change", e)
        }

        overlayManager?.onConfigurationChanged(screenWidth, screenHeight)
    }

    private var lastDetectedSubtitleText = ""

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val imgWidth = image.width
            val imgHeight = image.height
            val rowPadding = rowStride - pixelStride * imgWidth

            if (rowPadding == 0) {
                val bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                bitmap
            } else {
                val rawBitmap = Bitmap.createBitmap(
                    imgWidth + rowPadding / pixelStride,
                    imgHeight,
                    Bitmap.Config.ARGB_8888
                )
                rawBitmap.copyPixelsFromBuffer(buffer)
                val croppedBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, imgWidth, imgHeight)
                rawBitmap.recycle()
                croppedBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Image to Bitmap", e)
            null
        }
    }

    private suspend fun processCapturedBitmap(bitmap: Bitmap, isAutoMode: Boolean): Boolean {
        val shouldFilter = isAutoMode && settingsRepository.filterEmptyScreenshots.value
        if (shouldFilter) {
            val region = settingsRepository.ocrRegion.value
            val ocrResult = ocrDetector.detectSubtitles(bitmap, region)
            if (!ocrResult.hasSubtitles) {
                Log.d(TAG, "Auto-capture: No English subtitles detected in frame, skipping save.")
                bitmap.recycle()
                return false
            }

            // Duplicate subtitle filtering
            if (settingsRepository.skipDuplicateSubtitles.value && lastDetectedSubtitleText.isNotBlank()) {
                if (ocrDetector.isDuplicate(lastDetectedSubtitleText, ocrResult.detectedText)) {
                    Log.d(TAG, "Auto-capture: Duplicate subtitle frame detected ('${ocrResult.detectedText}'), skipping save.")
                    bitmap.recycle()
                    return false
                }
            }
            lastDetectedSubtitleText = ocrResult.detectedText
            Log.d(TAG, "Auto-capture: Subtitles detected (${ocrResult.englishWordCount} English words: '${ocrResult.detectedText}'). Saving frame.")
        }

        triggerHapticFeedback()
        screenshotStorage.saveScreenshot(bitmap)
        bitmap.recycle()
        withContext(Dispatchers.Main) {
            overlayManager?.onCaptureSuccess()
        }
        _serviceState.update {
            it.copy(
                capturedCount = it.capturedCount + 1,
                lastCaptureTimestamp = System.currentTimeMillis()
            )
        }
        return true
    }

    suspend fun executeCapture(isAutoMode: Boolean = false): Boolean {
        val bitmap = acquireLatestBitmap() ?: run {
            if (!isAutoMode) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScreenCaptureService, "Не удалось захватить кадр (попробуйте еще раз)", Toast.LENGTH_SHORT).show()
                }
            }
            return false
        }
        return processCapturedBitmap(bitmap, isAutoMode)
    }

    fun captureAndSave(isAutoMode: Boolean = false, onFinished: ((Boolean) -> Unit)? = null) {
        serviceScope.launch {
            val result = executeCapture(isAutoMode)
            onFinished?.invoke(result)
        }
    }

    private suspend fun acquireLatestBitmap(): Bitmap? = withContext(Dispatchers.Default) {
        val reader = imageReader ?: return@withContext null
        var image: Image? = null
        var attempts = 0

        // Retry loop to ensure a fresh image frame is available from VirtualDisplay
        while (image == null && attempts < 5) {
            image = reader.acquireLatestImage()
            if (image == null) {
                attempts++
                delay(40)
            }
        }

        if (image == null) return@withContext null

        try {
            imageToBitmap(image)
        } finally {
            image.close()
        }
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            // Ignore if vibration is restricted
        }
    }

    fun toggleAutoCapture() {
        val current = _serviceState.value.isAutoCapture
        if (current) {
            stopAutoCapture()
        } else {
            startAutoCapture()
        }
    }

    private fun startAutoCapture() {
        autoCaptureJob?.cancel()
        _serviceState.update { it.copy(isAutoCapture = true) }
        overlayManager?.updateAutoCaptureState(true)

        val isSmart = settingsRepository.smartDetectionEnabled.value
        val intervalSec = settingsRepository.autoCaptureIntervalSec.value
        if (isSmart) {
            updateNotification("Умный авто-захват: слежение за появлением субтитров")
        } else {
            updateNotification("Авто-захват включен (каждые ${String.format(java.util.Locale.US, "%.1f", intervalSec)} сек)")
        }

        diffDetector.reset()

        autoCaptureJob = serviceScope.launch(Dispatchers.Default) {
            var lastCaptureTimestamp = 0L
            val minCaptureCooldownMs = 1200L

            while (_serviceState.value.isAutoCapture) {
                // Never auto-capture while SubSnap itself is in foreground
                if (MainActivity.isAppInForeground) {
                    delay(300L)
                    continue
                }

                val smartMode = settingsRepository.smartDetectionEnabled.value
                if (!smartMode) {
                    val currentIntervalSec = settingsRepository.autoCaptureIntervalSec.value
                    val delayMs = (currentIntervalSec * 1000).toLong().coerceIn(1000L, 15000L)
                    delay(delayMs)
                    if (_serviceState.value.isAutoCapture && !MainActivity.isAppInForeground) {
                        executeCapture(isAutoMode = true)
                    }
                    continue
                }

                // Stage 1: Ultra-lightweight Subtitle Band Sampling (~180ms delay)
                delay(180L)
                if (!_serviceState.value.isAutoCapture || MainActivity.isAppInForeground) continue

                val region = settingsRepository.ocrRegion.value
                val bandTop = if (region == "FULL_SCREEN") 0.40f else 0.68f
                val bandBottom = 0.95f

                val now = System.currentTimeMillis()
                if (now - lastCaptureTimestamp < minCaptureCooldownMs) {
                    // In cooldown period: update baseline to match ongoing video/subtitles
                    val reader = imageReader
                    if (reader != null) {
                        val cooldownImage = try { reader.acquireLatestImage() } catch (e: Exception) { null }
                        if (cooldownImage != null) {
                            try {
                                val luma = diffDetector.sampleFromImage(cooldownImage, screenWidth, screenHeight, bandTop, bandBottom)
                                diffDetector.updateBaseline(luma)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error updating cooldown baseline", e)
                            } finally {
                                cooldownImage.close()
                            }
                        }
                    }
                    continue
                }

                val reader = imageReader ?: continue
                val image = try { reader.acquireLatestImage() } catch (e: Exception) { null } ?: continue
                var isSignificant = false
                var currentLuminance: IntArray? = null

                try {
                    currentLuminance = diffDetector.sampleFromImage(image, screenWidth, screenHeight, bandTop, bandBottom)
                    val diffResult = diffDetector.compare(currentLuminance)
                    isSignificant = diffResult.isSignificantChange
                    if (!isSignificant) {
                        // Smoothly adapt baseline to gradual background changes or subtitle disappearances
                        diffDetector.updateBaseline(currentLuminance)
                    } else {
                        Log.d(TAG, "SmartDetector: Subtitle appearance change detected (delta=%.2f, ratio=%.3f, brightened=%.3f). Debouncing 280ms...".format(
                            java.util.Locale.US, diffResult.meanLuminanceDelta, diffResult.changedFraction, diffResult.brightenedFraction
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in Stage 1 diff detection", e)
                } finally {
                    image.close()
                }

                if (isSignificant && _serviceState.value.isAutoCapture && !MainActivity.isAppInForeground) {
                    // Stage 2: Debounce 280ms for text rendering/animation to settle
                    delay(280L)
                    if (!_serviceState.value.isAutoCapture || MainActivity.isAppInForeground) break

                    var settledImage: Image? = null
                    var settleAttempts = 0
                    while (settledImage == null && settleAttempts < 4) {
                        settledImage = try { imageReader?.acquireLatestImage() } catch (e: Exception) { null }
                        if (settledImage == null) {
                            settleAttempts++
                            delay(30L)
                        }
                    }

                    if (settledImage != null) {
                        var settledBitmap: Bitmap? = null
                        try {
                            // Synchronize baseline with the exact settled frame before processing
                            val settledLuma = diffDetector.sampleFromImage(settledImage, screenWidth, screenHeight, bandTop, bandBottom)
                            diffDetector.updateBaseline(settledLuma)
                            settledBitmap = imageToBitmap(settledImage)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing settled frame", e)
                        } finally {
                            settledImage.close()
                        }

                        if (settledBitmap != null) {
                            val captured = processCapturedBitmap(settledBitmap, isAutoMode = true)
                            if (captured) {
                                lastCaptureTimestamp = System.currentTimeMillis()
                            }
                        }
                    } else {
                        val captured = executeCapture(isAutoMode = true)
                        if (captured) {
                            lastCaptureTimestamp = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }

    private fun stopAutoCapture() {
        autoCaptureJob?.cancel()
        autoCaptureJob = null
        diffDetector.reset()
        lastDetectedSubtitleText = ""
        _serviceState.update { it.copy(isAutoCapture = false) }
        overlayManager?.updateAutoCaptureState(false)
        updateNotification("Авто-захват выключен. Ручной режим.")
    }

    private fun setupOverlay() {
        overlayManager = OverlayManager(
            context = this,
            onCaptureClick = { captureAndSave() },
            onToggleAutoClick = { toggleAutoCapture() },
            onOpenAppClick = { openApp() },
            onCloseClick = { stopSelf() }
        ).apply {
            show()
            updateAutoCaptureState(_serviceState.value.isAutoCapture)
        }
    }

    private fun openApp() {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(appIntent)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this,
            102,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Остановить", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SubSnap Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление активного захвата экрана для SubSnap"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun cleanupProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        displayManager?.unregisterDisplayListener(displayListener)
        stopAutoCapture()
        cleanupProjection()
        stopBackgroundThread()
        overlayManager?.dismiss()
        overlayManager = null
        ocrDetector.close()
        serviceScope.cancel()
        _serviceState.value = ServiceState(isRunning = false, isAutoCapture = false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ScreenCaptureService"
        const val CHANNEL_ID = "subsnap_capture_channel"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START = "com.example.subsnap.action.START"
        const val ACTION_STOP = "com.example.subsnap.action.STOP"
        const val ACTION_TRIGGER_CAPTURE = "com.example.subsnap.action.CAPTURE"
        const val ACTION_TOGGLE_AUTO = "com.example.subsnap.action.TOGGLE_AUTO"
        const val ACTION_START_AUTO = "com.example.subsnap.action.START_AUTO"
        const val ACTION_STOP_AUTO = "com.example.subsnap.action.STOP_AUTO"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_START_AUTO = "extra_start_auto"

        data class ServiceState(
            val isRunning: Boolean = false,
            val isAutoCapture: Boolean = false,
            val capturedCount: Int = 0,
            val lastCaptureTimestamp: Long = 0L
        )

        private val _serviceState = MutableStateFlow(ServiceState())
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

        fun start(context: Context, resultCode: Int, resultData: Intent, autoStart: Boolean? = null) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                if (autoStart != null) {
                    putExtra(EXTRA_START_AUTO, autoStart)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun toggleAuto(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_TOGGLE_AUTO
            }
            context.startService(intent)
        }

        fun setAuto(context: Context, enabled: Boolean) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = if (enabled) ACTION_START_AUTO else ACTION_STOP_AUTO
            }
            context.startService(intent)
        }
    }
}
