package com.nouri.app.guardian

import java.util.UUID

/**
 * أنواع الجلسات المتاحة في تطبيق نوري
 */
enum class SessionType {
    QUICK_LOCK,  // قفل سريع بمؤقت
    SCHEDULED    // جلسة مجدولة
}

/**
 * أيام الأسبوع للجلسات المتكررة (1 = الأحد، 7 = السبت)
 */
enum class DayOfWeek(val value: Int) {
    SUNDAY(1), MONDAY(2), TUESDAY(3), WEDNESDAY(4), THURSDAY(5), FRIDAY(6), SATURDAY(7)
}

/**
 * نموذج بيانات جلسة التركيز / الحجب
 */
data class FocusSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "جلسة تركيز",
    val type: SessionType = SessionType.QUICK_LOCK,
    val durationMinutes: Int = 30,           // مدة القفل السريع
    val startTimeInMillis: Long = 0L,        // وقت بداية الجلسة
    val endTimeInMillis: Long = 0L,          // وقت نهاية الجلسة
    val startHour: Int = 0,                  // ساعة البداية للجدولة (0-23)
    val startMinute: Int = 0,                // دقيقة البداية للجدولة (0-59)
    val endHour: Int = 0,                    // ساعة النهاية للجدولة
    val endMinute: Int = 0,                  // دقيقة النهاية للجدولة
    val repeatDays: Set<DayOfWeek> = emptySet(), // أيام التكرار الأسبوعي
    val isRecurring: Boolean = false,        // هل الجلسة متكررة؟
    val isActive: Boolean = false,           // هل الجلسة شغالة حالياً؟
    val blockedPackages: Set<String> = emptySet() // قائمة حزم التطبيقات المحجوبة
) {
    /**
     * فحص ما إذا كانت الجلسة المجدولة نشطة في الوقت الحالي
     */
    fun isScheduledActiveNow(currentHour: Int, currentMinute: Int, currentDay: DayOfWeek): Boolean {
        if (!isActive) return false
        if (isRecurring && currentDay !in repeatDays) return false

        val currentTotalMinutes = currentHour * 60 + currentMinute
        val startTotalMinutes = startHour * 60 + startMinute
        val endTotalMinutes = endHour * 60 + endMinute

        return if (endTotalMinutes >= startTotalMinutes) {
            currentTotalMinutes in startTotalMinutes..endTotalMinutes
        } else {
            // الجلسة تمتد لمنتصف الليل (مثلاً من 10 مساءً حتى 2 صباحاً)
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes
        }
    }
}

