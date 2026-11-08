package com.example.hdcfuncap.features

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.hdcfuncap.components.HdcBottomBar
import com.example.hdcfuncap.notifications.PresetReminderScheduler
import com.example.hdcfuncap.notifications.MedicationNotificationScheduler
import com.example.hdcfuncap.notifications.ReminderRefreshWork
import com.example.hdcfuncap.storage.UserPreferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun LembretesScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val userPreferences = remember { UserPreferences(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var medicationEnabled by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var preciseAlarmsAllowed by remember { mutableStateOf(false) }
    var notificationsAllowed by remember { mutableStateOf(true) }
    val waterEnabled by userPreferences.waterRemindersEnabled.collectAsState(initial = true)
    val checkInEnabled by userPreferences.checkInRemindersEnabled.collectAsState(initial = true)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            ReminderRefreshWork.enqueue(appContext)
        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                preciseAlarmsAllowed = Build.VERSION.SDK_INT < 31 ||
                    appContext.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                notificationsAllowed = androidx.core.app.NotificationManagerCompat.from(appContext).areNotificationsEnabled()
                medicationEnabled = MedicationNotificationScheduler.isEnabled(appContext)
                ReminderRefreshWork.enqueue(appContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun savePreference(work: suspend () -> Unit) {
        if (saving) return
        saving = true
        coroutineScope.launch {
            try {
                withContext(NonCancellable + Dispatchers.IO) { work() }
                ReminderRefreshWork.enqueue(appContext)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ReminderSettings", "Unable to save reminder preference", e)
                snackbar.showSnackbar("Não foi possível atualizar o lembrete. Tente novamente.")
            } finally {
                saving = false
            }
        }
    }

    fun openSystemSettings(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: RuntimeException) {
            coroutineScope.launch { snackbar.showSnackbar("Não foi possível abrir as configurações do aparelho.") }
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            try {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (e: RuntimeException) {
                android.util.Log.w("ReminderSettings", "Unable to request notification permission", e)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            HdcBottomBar(
                currentScreen = "perfil",
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Lembretes",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Notificações locais",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Preferências deste dispositivo",
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    lineHeight = 19.sp
                )
            }

            item {
                ReminderToggleCard(
                    icon = Icons.Outlined.Medication,
                    iconTint = Color(0xFF6B9DFE),
                    iconBackground = Color(0xFFEAF1FF),
                    title = "Medicamentos",
                    subtitle = "Intervalos a partir do primeiro registro realizado",
                    checked = medicationEnabled,
                    enabled = !saving,
                    onCheckedChange = { enabled ->
                        if (enabled) requestNotificationPermissionIfNeeded()
                        savePreference {
                            MedicationNotificationScheduler.setEnabled(appContext, enabled)
                            withContext(Dispatchers.Main) { medicationEnabled = enabled }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ReminderToggleCard(
                    icon = Icons.Outlined.LocalDrink,
                    iconTint = Color(0xFF6B9DFE),
                    iconBackground = Color(0xFFEAF1FF),
                    title = "Beber água",
                    subtitle = "A cada 2 horas, das 08:00 às 22:00",
                    checked = waterEnabled,
                    enabled = !saving,
                    onCheckedChange = { enabled ->
                        if (enabled) requestNotificationPermissionIfNeeded()
                        savePreference {
                            userPreferences.saveWaterRemindersEnabled(enabled)
                            PresetReminderScheduler.setWaterRemindersEnabled(appContext, enabled)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ReminderToggleCard(
                    icon = Icons.Outlined.NotificationsActive,
                    iconTint = Color(0xFF57C47A),
                    iconBackground = Color(0xFFE8F7ED),
                    title = "Lembretes pontuais",
                    subtitle = "Todos os dias às 09:00, 12:00 e 21:00",
                    checked = checkInEnabled,
                    enabled = !saving,
                    onCheckedChange = { enabled ->
                        if (enabled) requestNotificationPermissionIfNeeded()
                        savePreference {
                            userPreferences.saveCheckInRemindersEnabled(enabled)
                            PresetReminderScheduler.setCheckInRemindersEnabled(appContext, enabled)
                        }
                    }
                )
            }
            if (!notificationsAllowed) {
                item {
                    TextButton(onClick = {
                        val settingsIntent = if (Build.VERSION.SDK_INT >= 26) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        }
                        openSystemSettings(settingsIntent)
                    }) {
                        Icon(Icons.Outlined.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Notificações bloqueadas no aparelho")
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 31 && !preciseAlarmsAllowed) {
                item {
                    TextButton(onClick = {
                        openSystemSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")))
                    }) {
                        Icon(Icons.Outlined.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Permitir horários precisos para medicamentos")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderToggleCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF6B9DFE),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCBD5E1),
                    uncheckedBorderColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
}
