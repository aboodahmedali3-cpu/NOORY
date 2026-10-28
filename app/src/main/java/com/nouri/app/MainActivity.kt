package com.nouri.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.nouri.app.guardian.DEFAULT_DISTRACTIONS
import com.nouri.app.guardian.GuardianAccessibilityService
import com.nouri.app.guardian.GuardianStore
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        GuardianScreen()
                    }
                }
            }
        }
    }
}

private fun isServiceEnabled(context: android.content.Context): Boolean {
    val expected = ComponentName(context, GuardianAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

@Composable
fun GuardianScreen() {
    val context = LocalContext.current
    val store = remember { GuardianStore(context) }

    var serviceOn by remember { mutableStateOf(isServiceEnabled(context)) }
    var limit by remember { mutableIntStateOf(store.dailyLimitMinutes) }
    var focus by remember { mutableStateOf(store.focusMode) }
    var refresh by remember { mutableIntStateOf(0) }

    // أعد الفحص عند العودة من شاشة إعدادات إمكانية الوصول.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceOn = isServiceEnabled(context)
                refresh++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("نوري — الحارس", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (serviceOn) "الحارس يعمل ✅" else "الحارس متوقف ⚠️",
                    style = MaterialTheme.typography.titleMedium
                )
                if (!serviceOn) {
                    Text("فعّل «نوري» من إعدادات إمكانية الوصول ليبدأ الحجب.")
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }) { Text("فتح إعدادات إمكانية الوصول") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("السقف اليومي لكل تطبيق", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = {
                        limit = (limit - 5).coerceAtLeast(5); store.dailyLimitMinutes = limit
                    }) { Text("−5") }
                    Text("$limit دقيقة", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = {
                        limit = (limit + 5).coerceAtMost(600); store.dailyLimitMinutes = limit
                    }) { Text("+5") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("وضع التركيز", style = MaterialTheme.typography.titleMedium)
                    Text("حجب كل التطبيقات المشتتة الآن")
                }
                Switch(checked = focus, onCheckedChange = {
                    focus = it; store.focusMode = it
                })
            }
        }

        Text("استخدام اليوم", style = MaterialTheme.typography.titleMedium)
        // `refresh` يجبر إعادة القراءة عند العودة للتطبيق.
        key(refresh) {
            DEFAULT_DISTRACTIONS.forEach { (pkg, name) ->
                val minutes = store.usedMillisToday(pkg) / 60_000
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name)
                    Text("$minutes / $limit د")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
