package com.nouri.app.guardian

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * خدمة الحراسة:
 *  - تتتبع التطبيق المفتوح حالياً (اسم الحزمة فقط، دون قراءة محتوى الشاشة).
 *  - تحتسب مدة الاستخدام لكل تطبيق مشتت.
 *  - تعيد المستخدم للشاشة الرئيسية عند تجاوز السقف أو في وضع التركيز.
 */
class GuardianAccessibilityService : AccessibilityService() {

    private lateinit var store: GuardianStore
    private val handler = Handler(Looper.getMainLooper())

    private var currentPkg: String? = null
    private var sinceElapsed: Long = 0L

    /** فحص دوري كل 5 ثوانٍ طالما تطبيق مشتت في المقدمة، لقطع الجلسة عند بلوغ السقف. */
    private val ticker = object : Runnable {
        override fun run() {
            val pkg = currentPkg
            if (pkg != null && pkg in DEFAULT_DISTRACTIONS) {
                flushUsage()
                if (store.isBlocked(pkg)) kick()
                handler.postDelayed(this, TICK_MS)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        store = GuardianStore(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == currentPkg) return
        if (isSystemOverlay(pkg)) return

        flushUsage()
        currentPkg = pkg
        sinceElapsed = SystemClock.elapsedRealtime()
        handler.removeCallbacks(ticker)

        if (pkg in DEFAULT_DISTRACTIONS) {
            if (store.isBlocked(pkg)) {
                kick()
            } else {
                handler.postDelayed(ticker, TICK_MS)
            }
        }
    }

    override fun onInterrupt() {
        flushUsage()
        handler.removeCallbacks(ticker)
    }

    override fun onDestroy() {
        flushUsage()
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    /** يضيف الوقت المنقضي للتطبيق الحالي إلى سجل اليوم. */
    private fun flushUsage() {
        val pkg = currentPkg ?: return
        val now = SystemClock.elapsedRealtime()
        if (pkg in DEFAULT_DISTRACTIONS) {
            store.addUsage(pkg, now - sinceElapsed)
        }
        sinceElapsed = now
    }

    private fun kick() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        currentPkg = null
        handler.removeCallbacks(ticker)
    }

    /** نوافذ النظام (لوحة الإشعارات، لوحة المفاتيح...) لا تُعدّ تغييراً للتطبيق. */
    private fun isSystemOverlay(pkg: String): Boolean =
        pkg == "com.android.systemui" || pkg.contains("inputmethod") || pkg.contains("keyboard")

    private companion object {
        const val TICK_MS = 5_000L
    }
}
