package com.nouri.app.guardian

import android.content.Context
import android.content.SharedPreferences

val DEFAULT_DISTRACTIONS = listOf(
    "com.instagram.android",
    "com.facebook.katana",
    "com.zhiliaoapp.musically", // TikTok
    "com.google.android.youtube",
    "com.whatsapp",
    "com.telegram.messenger"
)

object GuardianStore {
    private const val PREF_NAME = "nouri_guardian_prefs"
    private const val KEY_IS_LOCKED = "is_locked"
    private const val KEY_DAILY_LIMIT = "daily_limit_minutes"
    private const val KEY_DISTRACTIONS = "distractions_list"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isLocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOCKED, false)
    }

    fun setLocked(context: Context, locked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOCKED, locked).apply()
    }

    fun getDailyLimit(context: Context): Int {
        return getPrefs(context).getInt(KEY_DAILY_LIMIT, 5) // الافتراضي 5 دقائق كما ظهر في تصميمك
    }

    fun setDailyLimit(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_DAILY_LIMIT, minutes).apply()
    }

    fun getDistractions(context: Context): List<String> {
        val saved = getPrefs(context).getString(KEY_DISTRACTIONS, null)
        if (saved.isNullOrEmpty()) {
            return DEFAULT_DISTRACTIONS
        }
        return saved.split(",")
    }

    fun setDistractions(context: Context, list: List<String>) {
        val joined = list.joinToString(",")
        getPrefs(context).edit().putString(KEY_DISTRACTIONS, joined).apply()
    }
}
