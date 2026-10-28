package com.nouri.app.guardian

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class GuardianAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 3000L

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkCurrentApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "خدمة نوري للحظر تعمل بنجاح 🛡️", Toast.LENGTH_SHORT).show()
        handler.post(monitorRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            checkCurrentApp()
        }
    }

    private fun checkCurrentApp() {
        val context = applicationContext
        val packageName = rootInActiveWindow?.packageName?.toString() ?: return

        if (packageName == "com.nouri.app") return
        if (GuardianStore.isPackageWhitelisted(context, packageName)) return

        val focusEnabled = GuardianStore.isLocked(context)
        val dailyLimitReached = GuardianStore.isDailyLimitReached(context)
        val scheduledFocusActive = GuardianStore.isScheduledFocusActive(context)

        if (!focusEnabled && !dailyLimitReached && !scheduledFocusActive) return
        if (!isDistractionApp(context, packageName)) return

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        startActivity(homeIntent)
        GuardianStore.setLastBlockedApp(context, packageName)
        GuardianStore.recordPackageUsage(context, packageName, 1)

        Toast.makeText(context, "محظور بسبب جلسة التركيز 🔒", Toast.LENGTH_SHORT).show()
    }

    private fun isDistractionApp(context: Context, packageName: String): Boolean {
        val target = packageName.trim()
        if (target.isEmpty()) return false

        return GuardianStore.getDistractions(context).any { distraction ->
            distraction.equals(target, ignoreCase = true) ||
                target.contains(distraction, ignoreCase = true) ||
                distraction.contains(target, ignoreCase = true)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)
    }
}

