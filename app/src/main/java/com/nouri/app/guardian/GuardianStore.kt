package com.nouri.app.guardian

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

val DEFAULT_DISTRACTIONS = listOf(
    "com.instagram.android",
    "com.facebook.katana",
    "com.zhiliaoapp.musically",
    "com.google.android.youtube",
    "com.whatsapp",
    "com.telegram.messenger"
)

object GuardianStore {
    private const val PREF_NAME = "nouri_guardian_prefs"
    private const val KEY_IS_LOCKED = "is_locked"
    private const val KEY_DAILY_LIMIT = "daily_limit_minutes"
    private const val KEY_DISTRACTIONS = "distractions_list"
    private const val KEY_DAILY_USAGE = "daily_usage_minutes"
    private const val KEY_LAST_RESET_DATE = "last_reset_date"
    private const val KEY_PACKAGE_USAGE = "package_usage_map"
    private const val DEFAULT_DAILY_LIMIT = 5

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isLocked(context: Context): Boolean {
        resetIfNeeded(context)
        return getPrefs(context).getBoolean(KEY_IS_LOCKED, false)
    }

    fun setLocked(context: Context, locked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOCKED, locked).apply()
    }

    fun getDailyLimit(context: Context): Int {
        resetIfNeeded(context)
        return getPrefs(context).getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT).coerceAtLeast(0)
    }

    fun setDailyLimit(context: Context, minutes: Int) {
        val safeMinutes = minutes.coerceAtLeast(0)
        getPrefs(context).edit().putInt(KEY_DAILY_LIMIT, safeMinutes).apply()
    }

    fun getDistractions(context: Context): List<String> {
        val saved = getPrefs(context).getString(KEY_DISTRACTIONS, null)
        if (saved.isNullOrEmpty()) {
            return DEFAULT_DISTRACTIONS
        }
        return saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setDistractions(context: Context, list: List<String>) {
        val joined = list.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")
        getPrefs(context).edit().putString(KEY_DISTRACTIONS, joined).apply()
    }

    fun getDailyUsage(context: Context): Int {
        resetIfNeeded(context)
        return getPrefs(context).getInt(KEY_DAILY_USAGE, 0).coerceAtLeast(0)
    }

    fun setDailyUsage(context: Context, minutes: Int) {
        val safeMinutes = minutes.coerceAtLeast(0)
        getPrefs(context).edit().putInt(KEY_DAILY_USAGE, safeMinutes).apply()
    }

    fun addDailyUsage(context: Context, minutes: Int) {
        val safeMinutes = minutes.coerceAtLeast(0)
        val currentUsage = getDailyUsage(context)
        setDailyUsage(context, currentUsage + safeMinutes)
    }

    fun isDailyLimitReached(context: Context): Boolean {
        resetIfNeeded(context)
        return getDailyUsage(context) >= getDailyLimit(context)
    }

    fun getPackageUsage(context: Context, packageName: String): Int {
        val safeName = packageName.trim()
        if (safeName.isEmpty()) return 0
        return getPackageUsageMap(context)[safeName]?.coerceAtLeast(0) ?: 0
    }

    fun setPackageUsage(context: Context, packageName: String, minutes: Int) {
        val safeName = packageName.trim()
        if (safeName.isEmpty()) return

        val map = getPackageUsageMap(context).toMutableMap()
        map[safeName] = minutes.coerceAtLeast(0)
        getPrefs(context).edit().putString(KEY_PACKAGE_USAGE, encodePackageUsageMap(map)).apply()
    }

    fun recordPackageUsage(context: Context, packageName: String, minutes: Int) {
        val safeMinutes = minutes.coerceAtLeast(0)
        val adjustedPackage = packageName.trim()
        if (safeMinutes <= 0 || adjustedPackage.isEmpty()) return

        val current = getPackageUsage(context, adjustedPackage)
        setPackageUsage(context, adjustedPackage, current + safeMinutes)
        addDailyUsage(context, safeMinutes)
    }

    private fun resetIfNeeded(context: Context) {
        val prefs = getPrefs(context)
        val today = LocalDate.now().toString()
        val lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, null)

        if (lastResetDate != today) {
            prefs.edit()
                .putString(KEY_LAST_RESET_DATE, today)
                .putInt(KEY_DAILY_USAGE, 0)
                .putString(KEY_PACKAGE_USAGE, "")
                .apply()
        }
    }

    private fun getPackageUsageMap(context: Context): Map<String, Int> {
        val raw = getPrefs(context).getString(KEY_PACKAGE_USAGE, "") ?: ""
        if (raw.isBlank()) return emptyMap()

        return raw.split(";")
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().toIntOrNull() ?: 0
                    key to value.coerceAtLeast(0)
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun encodePackageUsageMap(map: Map<String, Int>): String {
        return map.entries
            .filter { it.key.isNotBlank() }
            .joinToString(";") { "${it.key}=${it.value}" }
    }
}
