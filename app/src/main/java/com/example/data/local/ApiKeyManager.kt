package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_lecture_settings_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String {
        val userSavedKey = prefs.getString("custom_gemini_api_key", "")?.trim() ?: ""
        if (userSavedKey.isNotEmpty()) {
            return userSavedKey
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove("custom_gemini_api_key").apply()
    }

    fun hasValidKey(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    fun isUsingCustomKey(): Boolean {
        val userSavedKey = prefs.getString("custom_gemini_api_key", "")?.trim() ?: ""
        return userSavedKey.isNotEmpty()
    }
}
