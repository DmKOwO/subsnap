package com.example.subsnap.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("subsnap_settings", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_API_KEY, "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(
        prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    )
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _filterEmptyScreenshots = MutableStateFlow(
        prefs.getBoolean(KEY_FILTER_EMPTY, true)
    )
    val filterEmptyScreenshots: StateFlow<Boolean> = _filterEmptyScreenshots.asStateFlow()

    fun setApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
        _apiKey.value = trimmed
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _selectedModel.value = model
    }

    fun setFilterEmptyScreenshots(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FILTER_EMPTY, enabled).apply()
        _filterEmptyScreenshots.value = enabled
    }

    val isConfigured: Boolean
        get() = _apiKey.value.isNotBlank()

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_FILTER_EMPTY = "filter_empty_screenshots"
        const val DEFAULT_MODEL = "gemini-2.5-flash"

        val AVAILABLE_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
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
