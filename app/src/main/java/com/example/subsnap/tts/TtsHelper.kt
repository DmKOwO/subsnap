package com.example.subsnap.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsHelper(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentSpeechRate = 0.95f
    private var currentLocale = Locale.US

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to UK English or default
                    val ukResult = tts?.setLanguage(Locale.UK)
                    if (ukResult == TextToSpeech.LANG_MISSING_DATA || ukResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.ENGLISH)
                    }
                }
                tts?.setSpeechRate(currentSpeechRate)
                tts?.setPitch(1.0f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        Log.w(TAG, "TTS playback error code: $errorCode")
                    }
                })
                isInitialized = true
                Log.d(TAG, "TTS initialized successfully with US/UK English")
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.7f, 1.5f)
        currentSpeechRate = clamped
        tts?.setSpeechRate(clamped)
    }

    fun setAccent(accent: String) {
        val targetLocale = if (accent.equals("UK", ignoreCase = true)) Locale.UK else Locale.US
        currentLocale = targetLocale
        tts?.let {
            val res = it.setLanguage(targetLocale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                it.setLanguage(Locale.ENGLISH)
            }
        }
    }

    fun speak(text: String, rate: Float? = null, accent: String? = null) {
        if (text.isBlank()) return
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not yet initialized, speech requested: $text")
            return
        }

        rate?.let { setSpeechRate(it) }
        accent?.let { setAccent(it) }

        val utteranceId = "tts_${UUID.randomUUID()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }

    companion object {
        private const val TAG = "TtsHelper"

        @Volatile
        private var instance: TtsHelper? = null

        fun getInstance(context: Context): TtsHelper {
            return instance ?: synchronized(this) {
                instance ?: TtsHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}
