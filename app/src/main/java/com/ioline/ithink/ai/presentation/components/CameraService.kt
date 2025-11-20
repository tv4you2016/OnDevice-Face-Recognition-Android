package com.ioline.ithink.ai.presentation.components

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.Image
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.Executors
import kotlin.math.abs
import android.R
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import com.ioline.ithink.ai.settingsdatastore.AppSettings
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first


class CameraService : LifecycleService() {

    private val TAG = "CameraService"
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val width = 160
    private val height = 120
    private var lastFrameGray: ByteArray? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var sensorReference: Float = 1000f // valor inicial provisório

    private val sensorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            sensorReference = intent?.getFloatExtra("sensorReference", 1000f) ?: 1000f
        }
    }


    override fun onCreate() {
        super.onCreate()

        // Inicializa sensorReference a partir do DataStore
        initializeSensorReference()

        registerReceiver(
            sensorReceiver,
            IntentFilter("UPDATE_SENSOR_REFERENCE"),
            Context.RECEIVER_EXPORTED
        )

        startForegroundService()
        startCameraAnalysis()
    }

    private fun initializeSensorReference() {
        // Cria instância do SettingsDataStore
        val settingsStore = SettingsDataStore(this)

        // Lê o valor salvo de forma assíncrona
        lifecycleScope.launch {
            val currentSettings = settingsStore.settingsFlow.first() // pega o último valor
            sensorReference = currentSettings.camera.sensitivity * 10000f
            Log.d("CameraService", "SensorReference inicializado: $sensorReference")
        }
    }


    private fun startForegroundService() {
        val channelId = "AI_CAMERA_CHANNEL"
        val channelName = "AI Camera Processing"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW
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

    override fun onBind(intent: Intent) = super.onBind(intent)

    private fun startCameraAnalysis() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val analysisUseCase = ImageAnalysis.Builder()
                .setTargetResolution(Size(width, height))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysisUseCase.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    handleImageProxy(imageProxy)
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase)
            } catch (e: Exception) {
                Log.e(TAG, "Falha bind camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun handleImageProxy(imageProxy: ImageProxy) {
        val image = imageProxy.image ?: return
        val gray = yuvToGray(image, width, height)
        val smoothGray = smoothGrayImage(gray, width, height)

        if (lastFrameGray == null) {
            lastFrameGray = smoothGray
            return
        }

        val blockDiff = computeBlockDiff(lastFrameGray!!, smoothGray, width, height)
        val objectSize = computeObjectSize(lastFrameGray!!, smoothGray)

        lastFrameGray = smoothGray

        val proximityLevel = if (objectSize > sensorReference) "Próximo" else "Distante"


        Log.d(TAG, "sensorReference: $sensorReference blockDiff: $blockDiff, objectSize: $objectSize Status → $proximityLevel")

        // Envia broadcast para Compose ou outro listener
        val intent = Intent("CAMERA_SENSOR_UPDATE")

       // intent.putExtra("blockDiff", blockDiff)
       // intent.putExtra("objectSize", objectSize)

        intent.putExtra("proximityLevel", proximityLevel)
        sendBroadcast(intent)
    }

    private fun computeObjectSize(a: ByteArray, b: ByteArray): Int {
        var changedPixels = 0
        for (i in a.indices) {
            if (abs((a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)) > 20) {
                changedPixels++
            }
        }
        return changedPixels
    }

    private fun smoothGrayImage(input: ByteArray, w: Int, h: Int): ByteArray {
        val output = ByteArray(input.size)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                val sum = (
                        (input[idx].toInt() and 0xFF) +
                                (input[idx - 1].toInt() and 0xFF) +
                                (input[idx + 1].toInt() and 0xFF) +
                                (input[idx - w].toInt() and 0xFF) +
                                (input[idx + w].toInt() and 0xFF)
                        ) / 5
                output[idx] = sum.toByte()
            }
        }
        return output
    }

    private fun yuvToGray(image: Image, dstW: Int, dstH: Int): ByteArray {
        val yPlane = image.planes[0]
        val yBuf = yPlane.buffer
        val srcW = image.width
        val srcH = image.height
        val out = ByteArray(dstW * dstH)
        val stepX = srcW / dstW
        val stepY = srcH / dstH
        var idx = 0
        for (j in 0 until dstH) {
            val sy = j * stepY
            for (i in 0 until dstW) {
                val sx = i * stepX
                val pos = sy * srcW + sx
                val yVal = yBuf.get(pos).toInt() and 0xFF
                out[idx++] = yVal.toByte()
            }
        }
        return out
    }

    private fun computeBlockDiff(a: ByteArray, b: ByteArray, w: Int, h: Int): Int {
        val blockSize = 8
        var blockChanges = 0
        for (y in 0 until h step blockSize) {
            for (x in 0 until w step blockSize) {
                var sum = 0
                for (j in 0 until blockSize) {
                    for (i in 0 until blockSize) {
                        val xi = x + i
                        val yj = y + j
                        if (xi < w && yj < h) {
                            val idx = yj * w + xi
                            sum += abs((a[idx].toInt() and 0xFF) - (b[idx].toInt() and 0xFF))
                        }
                    }
                }
                if (sum > 1000) blockChanges++
            }
        }
        return blockChanges
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(sensorReceiver)
        cameraExecutor.shutdown()
        wakeLock?.let { if (it.isHeld) it.release() }
    }
}
