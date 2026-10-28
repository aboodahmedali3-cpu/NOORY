package com.nouri.app.guardian

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.nouri.app.MainActivity

class GuardianAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 3000L // كل 3 ثوانٍ

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkCurrentApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "خدمة الحراسة لنوري تعمل بنجاح", Toast.LENGTH_SHORT).show()
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

        if (isLocked) {
            val packageName = rootInActiveWindow?.packageName?.toString() ?: return
            val distractions = GuardianStore.getDistractions(context)

            // استثناء حزمة التطبيق نفسه وواجهات النظام
            if (packageName != "com.nouri.app" && 
                !packageName.contains("launcher") && 
                !packageName.contains("systemui") && 
                distractions.contains(packageName)) {
                
                // إعادة المستخدم إلى الشاشة الرئيسية للهاتف فوراً
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                
                Toast.makeText(context, "ممنوع! التطبيق محجوب حالياً بواسطة نوري 🛡️", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)
    }
}
