package com.nouri.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nouri.app.guardian.GuardianStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isLocked by remember(context) { mutableStateOf(GuardianStore.isLocked(context)) }
    var isLockedMode by remember(context) { mutableStateOf(GuardianStore.isLockedMode(context)) }
    var dailyLimit by remember(context) { mutableIntStateOf(GuardianStore.getDailyLimit(context)) }
    var dailyUsage by remember(context) { mutableIntStateOf(GuardianStore.getDailyUsage(context)) }
    var sessionDuration by remember(context) { mutableIntStateOf(GuardianStore.getSessionDuration(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLocked = GuardianStore.isLocked(context)
                isLockedMode = GuardianStore.isLockedMode(context)
                dailyLimit = GuardianStore.getDailyLimit(context)
                dailyUsage = GuardianStore.getDailyUsage(context)
                sessionDuration = GuardianStore.getSessionDuration(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val appList = listOf(
        "com.instagram.android" to "إنستغرام",
        "com.facebook.katana" to "فيسبوك",
        "com.zhiliaoapp.musically" to "تيك توك",
        "com.google.android.youtube" to "يوتيوب",
        "com.whatsapp" to "واتساب",
        "com.telegram.messenger" to "تيليجرام",
        "org.telegram.messenger" to "تيليجرام",
        "com.android.chrome" to "كروم"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "نوري – Focus Guardian",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLocked) "الحارس مفعل ✅" else "الحارس متوقف ⚠️",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLocked) "جميع المحظورات مفعلة الآن. حافظ على تركيزك." else "فعّل الحظر لبدء جلسة تركيز جديدة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "فتح إعدادات إمكانية الوصول")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "جلسة تركيز سريعة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(15, 30, 60, 120).forEach { minutes ->
                            Button(
                                onClick = {
                                    sessionDuration = minutes
                                    GuardianStore.setSessionDuration(context, minutes)
                                    GuardianStore.setLocked(context, true)
                                    isLocked = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "${minutes}د")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الحد اليومي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                        }) { Text("5-") }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "$dailyLimit دقيقة",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = {
                            dailyLimit += 5
                            GuardianStore.setDailyLimit(context, dailyLimit)
                        }) { Text("5+") }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "وضع الحظر القسري",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "منع إيقاف الحظر بسهولة",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isLockedMode,
                        onCheckedChange = { newState ->
                            GuardianStore.setLockedMode(context, newState)
                            isLockedMode = newState
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "قفل التطبيقات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "حظر كل التطبيقات المشتتة الآن",
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

            Text(
                text = "استخدام اليوم: $dailyUsage / $dailyLimit دقيقة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التطبيقات المشتتة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    appList.forEach { (packageName, appName) ->
                        val usage = GuardianStore.getPackageUsage(context, packageName)
                        val isBlocked = GuardianStore.getDistractions(context).any {
                            it.equals(packageName, ignoreCase = true)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = appName)
                                Text(
                                    text = "استخدام: $usage دقيقة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isBlocked,
                                onCheckedChange = { enabled ->
                                    GuardianStore.toggleDistraction(context, packageName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

