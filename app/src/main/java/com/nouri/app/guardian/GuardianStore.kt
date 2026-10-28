package com.nouri.app.guardian

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.LocalTime

val DEFAULT_DISTRACTIONS = listOf(
    "com.instagram.android",
    "com.facebook.katana",
    "com.zhiliaoapp.musically",
    "com.google.android.youtube",
    "com.whatsapp",
    "org.telegram.messenger",
    "com.telegram.messenger",
    "com.android.chrome"
)

object GuardianStore {
    private const val PREF_NAME = "nouri_guardian_prefs"
    private const val KEY_IS_LOCKED = "is_locked"
    private const val KEY_LOCKED_MODE = "locked_mode"
    private const val KEY_DAILY_LIMIT = "daily_limit_minutes"
    private const val KEY_SESSION_DURATION = "session_duration_minutes"
    private const val KEY_DISTRACTIONS = "distractions_list"
    private const val KEY_DAILY_USAGE = "daily_usage_minutes"
    private const val KEY_LAST_RESET_DATE = "last_reset_date"
    private const val KEY_PACKAGE_USAGE = "package_usage_map"
    private const val KEY_BLOCKED_APP = "last_blocked_app"
    private const val KEY_WEEKLY_HISTORY = "weekly_usage_history"
    private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
    private const val KEY_SCHEDULE_START_HOUR = "schedule_start_hour"
    private const val KEY_SCHEDULE_END_HOUR = "schedule_end_hour"
    private const val DEFAULT_DAILY_LIMIT = 30
    private const val DEFAULT_SESSION_DURATION = 30

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isLocked(context: Context): Boolean {
        resetIfNeeded(context)
        return getPrefs(context).getBoolean(KEY_IS_LOCKED, false)
    }

    fun setLocked(context: Context, locked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOCKED, locked).apply()
    }

    fun isLockedMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LOCKED_MODE, false)
    }

    fun setLockedMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCKED_MODE, enabled).apply()
    }

    fun getDailyLimit(context: Context): Int {
        resetIfNeeded(context)
        return getPrefs(context).getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT).coerceAtLeast(0)
    }

    fun setDailyLimit(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_DAILY_LIMIT, minutes.coerceAtLeast(0)).apply()
    }

    fun getSessionDuration(context: Context): Int {
        return getPrefs(context).getInt(KEY_SESSION_DURATION, DEFAULT_SESSION_DURATION).coerceAtLeast(5)
    }

    fun setSessionDuration(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_SESSION_DURATION, minutes.coerceAtLeast(5)).apply()
    }

    fun getDistractions(context: Context): List<String> {
        val saved = getPrefs(context).getString(KEY_DISTRACTIONS, null)
        if (saved.isNullOrEmpty()) {
            return DEFAULT_DISTRACTIONS
        }
        return saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setDistractions(context: Context, list: List<String>) {
        val joined = list.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")
        getPrefs(context).edit().putString(KEY_DISTRACTIONS, joined).apply()
    }

    fun toggleDistraction(context: Context, packageName: String): Boolean {
        val safeName = packageName.trim()
        if (safeName.isEmpty()) return false

        val current = getDistractions(context).toMutableList()
        if (current.any { it.equals(safeName, ignoreCase = true) }) {
            current.removeAll { it.equals(safeName, ignoreCase = true) }
            setDistractions(context, current)
            return false
        }

        current.add(safeName)
        setDistractions(context, current)
        return true
    }

    fun getDailyUsage(context: Context): Int {
        resetIfNeeded(context)
        return getPrefs(context).getInt(KEY_DAILY_USAGE, 0).coerceAtLeast(0)
    }

    fun setDailyUsage(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_DAILY_USAGE, minutes.coerceAtLeast(0)).apply()
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

    fun setLastBlockedApp(context: Context, packageName: String) {
        if (packageName.isBlank()) return
        getPrefs(context).edit().putString(KEY_BLOCKED_APP, packageName.trim()).apply()
    }

    fun getLastBlockedApp(context: Context): String {
        return getPrefs(context).getString(KEY_BLOCKED_APP, "") ?: ""
    }

    fun getWeeklyUsageHistory(context: Context): List<Int> {
        val raw = getPrefs(context).getString(KEY_WEEKLY_HISTORY, "") ?: ""
        if (raw.isBlank()) return listOf(0, 0, 0, 0, 0, 0, 0)

        return raw.split(",")
            .map { it.trim().toIntOrNull() ?: 0 }
            .takeIf { it.size == 7 }
            ?: listOf(0, 0, 0, 0, 0, 0, 0)
    }

    fun setScheduleEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply()
    }

    fun isScheduleEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SCHEDULE_ENABLED, false)
    }

    fun getScheduleStartHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_SCHEDULE_START_HOUR, 22)
    }

    fun setScheduleStartHour(context: Context, hour: Int) {
        getPrefs(context).edit().putInt(KEY_SCHEDULE_START_HOUR, hour.coerceIn(0, 23)).apply()
    }

    fun getScheduleEndHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_SCHEDULE_END_HOUR, 7)
    }

    fun setScheduleEndHour(context: Context, hour: Int) {
        getPrefs(context).edit().putInt(KEY_SCHEDULE_END_HOUR, hour.coerceIn(0, 23)).apply()
    }

    fun setScheduleWindow(context: Context, startHour: Int, endHour: Int) {
        setScheduleStartHour(context, startHour)
        setScheduleEndHour(context, endHour)
        setScheduleEnabled(context, true)
    }

    fun isScheduledFocusActive(context: Context): Boolean {
        if (!isScheduleEnabled(context)) return false

        val now = LocalTime.now()
        val currentMinutes = now.hour * 60 + now.minute
        val startMinutes = getScheduleStartHour(context) * 60
        val endMinutes = getScheduleEndHour(context) * 60

        return if (endMinutes >= startMinutes) {
            currentMinutes >= startMinutes && currentMinutes < endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    private fun resetIfNeeded(context: Context) {
        val prefs = getPrefs(context)
        val today = LocalDate.now().toString()
        val lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, null)

        if (lastResetDate != today) {
            val previousDailyUsage = prefs.getInt(KEY_DAILY_USAGE, 0)
            val weekly = getWeeklyUsageHistory(context).toMutableList().ifEmpty { MutableList(7) { 0 } }
            if (weekly.size == 7) {
                weekly.removeAt(0)
                weekly.add(previousDailyUsage)
                prefs.edit().putString(KEY_WEEKLY_HISTORY, weekly.joinToString(",")).apply()
            }

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

