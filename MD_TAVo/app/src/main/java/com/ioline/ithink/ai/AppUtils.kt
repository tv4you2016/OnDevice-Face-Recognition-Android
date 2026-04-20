// AppUtils.kt
package com.ioline.ithink.ai


import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.registerReceiver
import com.ioline.ithink.ai.presentation.components.AppLoading
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.ioline.ithink.ai.layout.Option
import com.ioline.ithink.ai.layout.startServiceIfNeeded
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first

object AppUtils {
    @Volatile
    var isAddUserFlowActive: Boolean = false

    fun isPlayStoreRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val tasks = activityManager.runningAppProcesses // ou runningTasks em versões antigas
        return tasks.any { it.processName == "com.android.vending" }
    }

    // --- Função para abrir Play Store ---
    @OptIn(DelicateCoroutinesApi::class)
    private fun openPlayStore(context: Context, packageName: String) {

        if (isPlayStoreRunning(context)) {
            Log.d("AppUtils", "Play Store já está aberta, não será aberta novamente.")
            return
        }

        // Atualiza o detectionType para Option.None
        // 🔥 Atualiza Option.None diretamente no DataStore
            val settingsStore = SettingsDataStore(context)
            GlobalScope.launch {
                val current = settingsStore.settingsFlow.first()
                val updated = current.copy(detectionType = Option.None)
                settingsStore.saveSettings(updated)
                Log.d("AppUtils", "DetectionType -> Option.None (forçado pelo openPlayStore)")

                // 🔥 Desliga qualquer serviço de detecção imediatamente
                startServiceIfNeeded(context, Option.None)

            }

        try {
            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Play Store não disponível, abrir via browser
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun openTargetAppSafe(context: Context, packageName: String, wakeLock: Boolean = true) {
        try {
            if (wakeLock) {
                WakeLock().wakeUpScreen(context)
                WakeLock().unlockScreen(context)
            }

            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                Log.d("AppUtils", "Tentando abrir app: $packageName")

                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(800)
                    try {
                        context.startActivity(launchIntent)
                        Log.d("AppUtils", "App aberto com sucesso: $packageName")
                    } catch (e: Exception) {
                        Log.e("AppUtils", "Erro ao abrir o app", e)
                    }
                }
            } else {
                openPlayStore(context, packageName)
            }
        } catch (e: Exception) {
            openPlayStore(context, packageName)
        }
    }

    @Composable
    fun getProximitySensorInfo(context: Context): Triple<String?, String?, Float?> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        return if (proximitySensor != null) {
            Log.d("ProximityInfo", "Nome do sensor: ${proximitySensor.name}")
            Log.d("ProximityInfo", "Fabricante: ${proximitySensor.vendor}")
            Log.d("ProximityInfo", "Alcance máximo: ${proximitySensor.maximumRange}")

            Triple(
                proximitySensor.name,    // sensorName
                proximitySensor.vendor,  // sensorVendor
                proximitySensor.maximumRange  // sensorMaxRange
            )
        } else {
            Triple(null, null, null) // Sensor não disponível
        }
    }




    fun startLoading(context: Context , local : String) {

        Log.d("Loading", "startLoading -> $local")
        val intent = Intent("GLOBAL_LOADING_UPDATE")
        intent.putExtra("loading", true)
        context.sendBroadcast(intent)
    }


    fun stopLoading(context: Context,  local : String) {
        Log.d("Loading", "stopLoading -> $local")
        val intent = Intent("GLOBAL_LOADING_UPDATE")
        intent.putExtra("loading", false)
        context.sendBroadcast(intent)
    }



}
