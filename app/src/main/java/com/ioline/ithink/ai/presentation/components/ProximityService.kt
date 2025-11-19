package com.ioline.ithink.ai.presentation.components

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log

class ProximityService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null


    companion object {
        var sensorName: String? = null
        var sensorVendor: String? = null
        var sensorMaxRange: Float? = null
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (proximitySensor != null) {
            // Salva os dados do sensor nas variáveis do companion object
            sensorName = proximitySensor?.name
            sensorVendor = proximitySensor?.vendor
            sensorMaxRange = proximitySensor?.maximumRange

            Log.d(
                "ProximityService",
                "📡 Sensor: Name=$sensorName, Vendor=$sensorVendor, MaxRange=$proximitySensor?.maximumRange"
            )

            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Log.d("ProximityService", "❌ Sensor de proximidade não disponível.")
            stopSelf()
        }
    }

    private fun startForegroundService() {
        val channelId = "AI_CAMERA_CHANNEL"
        val channelName = "AI Camera Processing"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notification: Notification =
            Notification.Builder(this, channelId)
                .setContentTitle("AI Camera Service")
                .setContentText("Processando vídeo da câmera...")
                .setSmallIcon(R.drawable.ic_menu_camera)
                .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("ProximityService", "🛑 Serviço de proximidade parado.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            Log.d("ProximityService", "Sensor: $distance cm / MaxRange: ${proximitySensor?.maximumRange}")

           val intent = Intent("PROXIMITY_SENSOR_UPDATE")
           intent.putExtra("distance", distance)
           sendBroadcast(intent)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null



}