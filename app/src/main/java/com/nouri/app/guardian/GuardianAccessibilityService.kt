package com.nouri.app.guardian

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class GuardianAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 2000L // الفحص كل ثانيتين

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkCurrentApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "خدمة حراسة نوري تعمل بذكاء 🛡️", Toast.LENGTH_SHORT).show()
        handler.post(monitorRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            checkCurrentApp()
        }
    }

    private fun checkCurrentApp() {
        val context = applicationContext
        val isLocked = GuardianStore.isLocked(context)

        // إذا كان وضع التركيز مفعلاً أو تم تجاوز السقف اليومي
        if (isLocked) {
            val packageName = rootInActiveWindow?.packageName?.toString() ?: return
            val distractions = GuardianStore.getDistractions(context)

            // استثناء تطبيقنا نوري وواجهات النظام الأساسية
            if (packageName != "com.nouri.app" &&
                !packageName.contains("launcher") &&
                !packageName.contains("systemui") &&
                distractions.any { packageName.contains(it) }) {

                // إرجاع المستخدم فوراً للشاشة الرئيسية
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)

                Toast.makeText(context, "محظور! حافظ على تركيزك يا بطل 📚", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)
    }
}
