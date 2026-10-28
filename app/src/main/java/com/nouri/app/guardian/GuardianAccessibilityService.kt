package com.nouri.app.guardian

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * خدمة الحراسة المحدثة:
 *  - تتتبع التطبيق المفتوح حالياً (اسم الحزمة فقط، دون قراءة محتوى الشاشة).
 *  - تحتسب مدة الاستخدام لكل تطبيق مشتت.
 *  - تعيد المستخدم للشاشة الرئيسية عند تجاوز السقف، أو في وضع التركيز، أو أثناء القفل السريع أو الجلسات النشطة.
 */
class GuardianAccessibilityService : AccessibilityService() {

    private lateinit var store: GuardianStore
    private val handler = Handler(Looper.getMainLooper())

    private var currentPkg: String? = null
    private var sinceElapsed: Long = 0L

    /** فحص دوري كل 5 ثوانٍ طالما تطبيق مشتت في المقدمة، لقطع الجلسة عند بلوغ السقف أو تفعيل القفل. */
    private val ticker = object : Runnable {
        override fun run() {
            val pkg = currentPkg
            if (pkg != null) {
                flushUsage()
                if (store.isBlocked(pkg)) {
                    kick()
                    showBlockedToast()
                } else {
                    handler.postDelayed(this, TICK_MS)
                }
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

        if (store.isBlocked(pkg)) {
            kick()
            showBlockedToast()
        } else if (pkg in DEFAULT_DISTRACTIONS) {
            handler.postDelayed(ticker, TICK_MS)
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

    private fun showBlockedToast() {
        Toast.makeText(applicationContext, "🚫 تم حجب التطبيق بواسطة تطبيق نوري", Toast.LENGTH_SHORT).show()
    }

    /** نوافذ النظام (لوحة الإشعارات، لوحة المفاتيح...) لا تُعدّ تغييراً للتطبيق. */
    private fun isSystemOverlay(pkg: String): Boolean =
        pkg == "com.android.systemui" || pkg.contains("inputmethod") || pkg.contains("keyboard")

    private companion object {
        const val TICK_MS = 5_000L
    }
}
