package com.nouri.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.nouri.app.guardian.GuardianAccessibilityService
import com.nouri.app.guardian.GuardianStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isLocked by remember { mutableStateOf(GuardianStore.isLocked(context)) }
    var dailyLimit by remember { mutableStateOf(GuardianStore.getDailyLimit(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLocked = GuardianStore.isLocked(context)
                dailyLimit = GuardianStore.getDailyLimit(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "نوري – الحارس",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // بطاقة تنبيه إمكانية الوصول
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "الحارس متوقف ⚠️",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "فعّل «نوري» من إعدادات إمكانية الوصول ليبدأ الحجب.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text(text = "فتح إعدادات إمكانية الوصول")
                }
            }
        }

        // بطاقة السقف اليومي
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "السقف اليومي لكل تطبيق",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = {
                        if (dailyLimit > 5) {
                            dailyLimit -= 5
                            GuardianStore.setDailyLimit(context, dailyLimit)
                        }
                    }) {
                        Text(text = "5-")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "$dailyLimit دقيقة",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = {
                        dailyLimit += 5
                        GuardianStore.setDailyLimit(context, dailyLimit)
                    }) {
                        Text(text = "5+")
                    }
                }
            }
        }

        // بطاقة وضع التركيز
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "وضع التركيز",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "حجب كل التطبيقات المشتتة الآن",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = isLocked,
                    onCheckedChange = { newState ->
                        GuardianStore.setLocked(context, newState)
                        isLocked = newState
                    }
                )
            }
        }

        // قسم استخدام اليوم
        Text(
            text = "استخدام اليوم",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.End).padding(vertical = 8.dp)
        )

        val apps = listOf("واتساب", "يوتيوب", "تيك توك", "تلجرام", "فيسبوك", "إنستجرام")
        apps.forEach { appName ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = appName)
                Text(text = "د 0 / $dailyLimit")
            }
        }
    }
}
