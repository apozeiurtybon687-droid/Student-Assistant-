package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserGender(val displayNameAr: String, val colorDescription: String) {
    MALE("طالب جامعي (ذكر)", "السمة الزرقاء الأكاديمية"),
    FEMALE("طالبة جامعية (فتاة)", "السمة الوردية الأنيقة")
}

enum class ThemeMode(val displayNameAr: String) {
    LIGHT("وضع نهاري (فاتح) ☀️"),
    DARK("وضع ليلي (داكن) 🌙"),
    SYSTEM("تلقائي (حسب النظام) ⚙️")
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    private val _gender = MutableStateFlow(loadGender())
    val gender: StateFlow<UserGender> = _gender.asStateFlow()

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun loadGender(): UserGender {
        val raw = prefs.getString(KEY_GENDER, UserGender.MALE.name)
        return try {
            UserGender.valueOf(raw ?: UserGender.MALE.name)
        } catch (_: Exception) {
            UserGender.MALE
        }
    }

    private fun loadThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(raw ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setGender(newGender: UserGender) {
        prefs.edit().putString(KEY_GENDER, newGender.name).apply()
        _gender.value = newGender
    }

    fun setThemeMode(newMode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, newMode.name).apply()
        _themeMode.value = newMode
    }

    companion object {
        private const val KEY_GENDER = "user_gender_preference"
        private const val KEY_THEME_MODE = "app_theme_mode_preference"

        @Volatile
        private var INSTANCE: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
