package com.ioline.ithink.ai
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log


import kotlinx.coroutines.*

class PeriodicAppLauncherService : Service() {

    private val resetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "RESET_PERIODIC_TIMER") {
                resetTimer()
            }
        }
    }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val interval = 60_000L // 1 minuto
    private val tickInterval = 1_000L // 1 segundo para log
    private var resetSignal = false

    fun resetTimer() {
        Log.d("PeriodicAppLauncher", "⏱ Timer resetado pelo toque na tela!")
        resetSignal = true
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()


        // ANDROID 14 FIX 🚨
        registerReceiver(
            resetReceiver,
            IntentFilter("RESET_PERIODIC_TIMER"),
            Context.RECEIVER_EXPORTED
        )

        // Inicia contagem periódica
        scope.launch {
            while (isActive) {
                var elapsed = 0L
                while (elapsed < interval) {
                    delay(tickInterval)
                    elapsed += tickInterval

                    // Se houver reset, reinicia contagem
                    if (resetSignal) {
                        Log.d("PeriodicAppLauncher", "⏱ Reset recebido! Reiniciando contagem")
                        elapsed = 0
                        resetSignal = false
                    }

                    val remaining = (interval - elapsed) / 1000
                    Log.d("PeriodicAppLauncher", "Tempo até o próximo lançamento: $remaining s")
                }

                // Dispara o app alvo
                Log.d("PeriodicAppLauncher", "🔔 Abrindo app alvo")
                AppUtils.openTargetApp(applicationContext, false)
            }
        }
    }

    private fun startForegroundWithNotification() {
        val channelId = "PERIODIC_APP_LAUNCHER"
        val channelName = "Periodic App Launcher"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("App Launcher Service")
                .setContentText("Executando app periodicamente")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("App Launcher Service")
                .setContentText("Executando app periodicamente")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        }

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        unregisterReceiver(resetReceiver)
        Log.d("PeriodicAppLauncher", "Serviço parado")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
