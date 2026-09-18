package com.example.subsnap.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("subsnap_settings", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_API_KEY, "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(getSanitizedModel())
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _filterEmptyScreenshots = MutableStateFlow(
        prefs.getBoolean(KEY_FILTER_EMPTY, true)
    )
    val filterEmptyScreenshots: StateFlow<Boolean> = _filterEmptyScreenshots.asStateFlow()

    private val _githubRepo = MutableStateFlow(
        prefs.getString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
    )
    val githubRepo: StateFlow<String> = _githubRepo.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(prefs.getFloat(KEY_TTS_SPEED, 1.0f))
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _ttsLocale = MutableStateFlow(prefs.getString(KEY_TTS_LOCALE, "US") ?: "US")
    val ttsLocale: StateFlow<String> = _ttsLocale.asStateFlow()

    private val _ocrRegion = MutableStateFlow(prefs.getString(KEY_OCR_REGION, "LOWER_THIRD") ?: "LOWER_THIRD")
    val ocrRegion: StateFlow<String> = _ocrRegion.asStateFlow()

    private val _skipDuplicateSubtitles = MutableStateFlow(
        prefs.getBoolean(KEY_SKIP_DUPLICATES, true)
    )
    val skipDuplicateSubtitles: StateFlow<Boolean> = _skipDuplicateSubtitles.asStateFlow()

    private val _autoCaptureIntervalSec = MutableStateFlow(
        prefs.getFloat(KEY_AUTO_CAPTURE_INTERVAL, DEFAULT_AUTO_CAPTURE_INTERVAL)
    )
    val autoCaptureIntervalSec: StateFlow<Float> = _autoCaptureIntervalSec.asStateFlow()

    private val _autoStartAutoCapture = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_START_CAPTURE, true)
    )
    val autoStartAutoCapture: StateFlow<Boolean> = _autoStartAutoCapture.asStateFlow()

    private val _smartDetectionEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SMART_DETECTION, true)
    )
    val smartDetectionEnabled: StateFlow<Boolean> = _smartDetectionEnabled.asStateFlow()

    private val _studyDailyGoal = MutableStateFlow(prefs.getInt(KEY_STUDY_DAILY_GOAL, 10))
    val studyDailyGoal: StateFlow<Int> = _studyDailyGoal.asStateFlow()

    private val _studyStreakDays = MutableStateFlow(prefs.getInt(KEY_STUDY_STREAK, 0))
    val studyStreakDays: StateFlow<Int> = _studyStreakDays.asStateFlow()

    private val _lastStudyDate = MutableStateFlow(prefs.getString(KEY_LAST_STUDY_DATE, "") ?: "")
    val lastStudyDate: StateFlow<String> = _lastStudyDate.asStateFlow()

    private val _autoPlayTts = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PLAY_TTS, false))
    val autoPlayTts: StateFlow<Boolean> = _autoPlayTts.asStateFlow()

    private val _clozeStudyMode = MutableStateFlow(prefs.getBoolean(KEY_CLOZE_STUDY_MODE, false))
    val clozeStudyMode: StateFlow<Boolean> = _clozeStudyMode.asStateFlow()

    private fun getSanitizedModel(): String {
        val saved = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        if (saved.startsWith("gemini-1.") ||
            saved.startsWith("gemini-2.") ||
            saved !in AVAILABLE_MODELS
        ) {
            prefs.edit().putString(KEY_MODEL, DEFAULT_MODEL).apply()
            return DEFAULT_MODEL
        }
        return saved
    }

    fun setApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
        _apiKey.value = trimmed
    }

    fun setSelectedModel(model: String) {
        val targetModel = if (model in AVAILABLE_MODELS) model else DEFAULT_MODEL
        prefs.edit().putString(KEY_MODEL, targetModel).apply()
        _selectedModel.value = targetModel
    }

    fun setFilterEmptyScreenshots(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FILTER_EMPTY, enabled).apply()
        _filterEmptyScreenshots.value = enabled
    }

    fun setGithubRepo(repo: String) {
        val trimmed = repo.trim()
        prefs.edit().putString(KEY_GITHUB_REPO, trimmed).apply()
        _githubRepo.value = trimmed
    }

    fun setTtsSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.7f, 1.5f)
        prefs.edit().putFloat(KEY_TTS_SPEED, clamped).apply()
        _ttsSpeed.value = clamped
    }

    fun setTtsLocale(locale: String) {
        val normalized = if (locale.equals("UK", ignoreCase = true)) "UK" else "US"
        prefs.edit().putString(KEY_TTS_LOCALE, normalized).apply()
        _ttsLocale.value = normalized
    }

    fun setOcrRegion(region: String) {
        val target = if (region.equals("FULL_SCREEN", ignoreCase = true)) "FULL_SCREEN" else "LOWER_THIRD"
        prefs.edit().putString(KEY_OCR_REGION, target).apply()
        _ocrRegion.value = target
    }

    fun setSkipDuplicateSubtitles(skip: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_DUPLICATES, skip).apply()
        _skipDuplicateSubtitles.value = skip
    }

    fun setAutoCaptureIntervalSec(interval: Float) {
        val clamped = interval.coerceIn(1.0f, 10.0f)
        prefs.edit().putFloat(KEY_AUTO_CAPTURE_INTERVAL, clamped).apply()
        _autoCaptureIntervalSec.value = clamped
    }

    fun setAutoStartAutoCapture(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START_CAPTURE, enabled).apply()
        _autoStartAutoCapture.value = enabled
    }

    fun setSmartDetectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_DETECTION, enabled).apply()
        _smartDetectionEnabled.value = enabled
    }

    fun setStudyDailyGoal(goal: Int) {
        val clamped = goal.coerceIn(5, 50)
        prefs.edit().putInt(KEY_STUDY_DAILY_GOAL, clamped).apply()
        _studyDailyGoal.value = clamped
    }

    fun setAutoPlayTts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY_TTS, enabled).apply()
        _autoPlayTts.value = enabled
    }

    fun setClozeStudyMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOZE_STUDY_MODE, enabled).apply()
        _clozeStudyMode.value = enabled
    }

    fun recordStudySessionComplete() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val lastDateStr = _lastStudyDate.value

        if (todayStr == lastDateStr) {
            // Already counted today
            return
        }

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val newStreak = if (lastDateStr == yesterdayStr) {
            _studyStreakDays.value + 1
        } else {
            1
        }

        prefs.edit()
            .putInt(KEY_STUDY_STREAK, newStreak)
            .putString(KEY_LAST_STUDY_DATE, todayStr)
            .apply()

        _studyStreakDays.value = newStreak
        _lastStudyDate.value = todayStr
    }

    val isConfigured: Boolean
        get() = _apiKey.value.isNotBlank()

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_FILTER_EMPTY = "filter_empty_screenshots"
        private const val KEY_GITHUB_REPO = "github_repo"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_TTS_LOCALE = "tts_locale"
        private const val KEY_OCR_REGION = "ocr_region"
        private const val KEY_SKIP_DUPLICATES = "skip_duplicate_subtitles"
        private const val KEY_AUTO_CAPTURE_INTERVAL = "auto_capture_interval_sec"
        private const val KEY_AUTO_START_CAPTURE = "auto_start_auto_capture"
        private const val KEY_SMART_DETECTION = "smart_subtitle_detection"
        private const val KEY_STUDY_DAILY_GOAL = "study_daily_goal"
        private const val KEY_STUDY_STREAK = "study_streak_days"
        private const val KEY_LAST_STUDY_DATE = "study_last_date"
        private const val KEY_AUTO_PLAY_TTS = "auto_play_tts"
        private const val KEY_CLOZE_STUDY_MODE = "cloze_study_mode"

        const val DEFAULT_GITHUB_REPO = "DmKOwO/subsnap"
        const val DEFAULT_AUTO_CAPTURE_INTERVAL = 2.5f
        const val DEFAULT_MODEL = "gemini-3.8-flash"

        val AVAILABLE_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-3.5-flash",
            "gemini-3.1-flash-lite",
            "gemini-flash-latest"
        )

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
