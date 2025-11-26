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
import androidx.core.content.ContextCompat.registerReceiver
import com.ioline.ithink.ai.presentation.components.AppLoading
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.ioline.ithink.ai.layout.Option
import com.ioline.ithink.ai.layout.startServiceIfNeeded
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first

object AppUtils {


    fun isPlayStoreRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val tasks = activityManager.runningAppProcesses // ou runningTasks em versões antigas
        return tasks.any { it.processName == "com.android.vending" }
    }

    // --- Função para abrir Play Store ---
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


    fun isAppRunning(context: Context, packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = activityManager.runningAppProcesses
        val isRunning = processes.any { it.processName == packageName }
        Log.d("AppUtils", "App $packageName está rodando? $isRunning")
        return isRunning
    }
    
    fun openTargetAppSafe(context: Context, packageName: String, wakeLock: Boolean = true) {
        if (isAppRunning(context, packageName)) {
            Log.d("AppUtils", "O app $packageName já está rodando, não será aberto novamente.")
            return
        }

        try {
            if (wakeLock) {
                // Acorda a tela se necessário
                WakeLock().wakeUpScreen(context)
                WakeLock().unlockScreen(context)
            }

            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                // Garantindo que será executado em nova task
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // Log para debug
                Log.d("AppUtils", "Tentando abrir app: $packageName")

                // Delay pequeno para garantir que a tela já acordou
                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(800) // 800ms funciona na maioria dos dispositivos
                    try {
                        context.startActivity(launchIntent)
                        Log.d("AppUtils", "App aberto com sucesso: $packageName")
                    } catch (e: Exception) {
                        Log.e("AppUtils", "Erro ao abrir o app", e)
                    }
                }
            } else {
                //Log.e("AppUtils", "App não encontrado ou Activity principal não exportada: $packageName")
                openPlayStore(context,packageName)

            }
        } catch (e: Exception) {
            //Log.e("AppUtils", "Erro geral ao tentar abrir app", e)
            openPlayStore(context,packageName)

        }
    }

    fun openTargetAppDelay(context: Context, wakeLock: Boolean) {
        if (wakeLock) {
            WakeLock().wakeUpScreen(context)
            WakeLock().unlockScreen(context)
        }

        Log.d("AppUtils", "WakeLock: $wakeLock")

        Handler(Looper.getMainLooper()).postDelayed({
            val launchIntent = context.packageManager.getLaunchIntentForPackage("app.ioline.ithink")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                } catch (e: Exception) {
                    Log.e("AppUtils", "Erro ao abrir o app", e)
                }
            } else {
                Log.e("AppUtils", "App não instalado ou sem Activity principal")
            }
        }, 1500)
    }

    fun openlockNowApp(context: Context) {


        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, MyDeviceAdminReceiver::class.java)

        if (dpm.isAdminActive(componentName)) {

            Handler(Looper.getMainLooper()).postDelayed({
                dpm.lockNow() // 🔒 desliga o ecrã
            }, 10000) // delay de 2 segundos para desligar o ecra

        } else {
            Log.e("ScreenControl", "Device Admin não está ativo.")
        }

    }

    fun hasProximitySensor(context: Context): Boolean {


        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager


        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        /*
        for (s in sensors) {
            Log.d("SensorList", "🔹 ${s.name} (${s.type}) range=${s?.maximumRange}")
        }
        */
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        return sensor != null && sensor.maximumRange > 0
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



    @Composable
    fun GlobalLoadingController(): State<Boolean> {
        val context = LocalContext.current
        val loadingState = remember { mutableStateOf(false) }


        DisposableEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action == "GLOBAL_LOADING_UPDATE") {
                        val isLoading = intent.getBooleanExtra("loading", false)
                        loadingState.value = isLoading
                    }
                }
            }

            val filter = IntentFilter("GLOBAL_LOADING_UPDATE")
            registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        return loadingState
    }


    @Composable
    fun LoadingScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AppLoading(size = 80.dp)
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
