package com.nouri.app.guardian

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

val DEFAULT_DISTRACTIONS = listOf(
    "com.instagram.android",
    "com.facebook.katana",
    "com.zhiliaoapp.musically", // TikTok
    "com.google.android.youtube"
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
        return getPrefs(context).getInt(KEY_DAILY_LIMIT, 60) // افتراضاً 60 دقيقة
    }

    fun setDailyLimit(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_DAILY_LIMIT, minutes).apply()
    }

    fun getDistractions(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_DISTRACTIONS, null) ?: return DEFAULT_DISTRACTIONS
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: DEFAULT_DISTRACTIONS
        } catch (e: Exception) {
            DEFAULT_DISTRACTIONS
        }
    }
}
