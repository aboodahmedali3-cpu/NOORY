package com.nouri.app.guardian

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/** التطبيقات المشتتة الافتراضية: الحزمة -> الاسم المعروض. */
val DEFAULT_DISTRACTIONS: Map<String, String> = linkedMapOf(
    "com.whatsapp" to "واتساب",
    "com.google.android.youtube" to "يوتيوب",
    "com.zhiliaoapp.musically" to "تيك توك",
    "org.telegram.messenger" to "تلجرام",
    "com.facebook.katana" to "فيسبوك",
    "com.instagram.android" to "إنستجرام",
    "com.android.chrome" to "كروم",
)

/**
 * تخزين محلي بسيط للإعدادات والاستخدام اليومي.
 * (المرحلة التالية: استبداله بـ Room + SQLCipher.)
 */
class GuardianStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("guardian", Context.MODE_PRIVATE)

    /** السقف اليومي بالدقائق لكل تطبيق مشتت. */
    var dailyLimitMinutes: Int
        get() = prefs.getInt(KEY_LIMIT, 30)
        set(v) = prefs.edit().putInt(KEY_LIMIT, v.coerceIn(1, 600)).apply()

    /** وضع التركيز: حجب كل التطبيقات المشتتة فوراً بغض النظر عن السقف. */
    var focusMode: Boolean
        get() = prefs.getBoolean(KEY_FOCUS, false)
        set(v) = prefs.edit().putBoolean(KEY_FOCUS, v).apply()

    fun usedMillisToday(pkg: String): Long {
        resetIfNewDay()
        return prefs.getLong(usageKey(pkg), 0L)
    }

    fun addUsage(pkg: String, millis: Long) {
        if (millis <= 0) return
        resetIfNewDay()
        prefs.edit().putLong(usageKey(pkg), usedMillisToday(pkg) + millis).apply()
    }

    fun isBlocked(pkg: String): Boolean {
        if (pkg !in DEFAULT_DISTRACTIONS) return false
        if (focusMode) return true
        return usedMillisToday(pkg) >= dailyLimitMinutes * 60_000L
    }

    private fun usageKey(pkg: String) = "usage_$pkg"

    private fun resetIfNewDay() {
        val today = LocalDate.now().toString()
        if (prefs.getString(KEY_DAY, null) != today) {
            val editor = prefs.edit()
            DEFAULT_DISTRACTIONS.keys.forEach { editor.remove(usageKey(it)) }
            editor.putString(KEY_DAY, today).apply()
        }
    }

    private companion object {
        const val KEY_LIMIT = "limit_minutes"
        const val KEY_FOCUS = "focus_mode"
        const val KEY_DAY = "day"
    }
}
