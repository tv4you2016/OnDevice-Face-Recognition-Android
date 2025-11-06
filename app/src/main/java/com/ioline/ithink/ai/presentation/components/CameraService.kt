package com.ioline.ithink.ai.presentation.components

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
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
import com.ioline.aicamera.utils.AppUtils.openTargetApp

class CameraService : LifecycleService() {
    private val TAG = "MotionDetectService"
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var lastFrameGray: ByteArray? = null
    private val width = 160  // downsized analysis resolution
    private val height = 120

    private var wakeLock: PowerManager.WakeLock? = null


    private var motionCount = 0
    private val requiredMotionFrames = 3
    private val blockMotionThreshold = 6  // nº mínimo de blocos que devem mudar


    override fun onCreate() {
        super.onCreate()
        startForegroundService();
        startCameraAnalysis()
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
        lastFrameGray = smoothGray


        if (blockDiff >= 5) {
            Log.d(TAG, "🔴 Objeto MUITO PERTO da câmera  blockDiff:$blockDiff ")

            openTargetApp(applicationContext,true)

        } else if (blockDiff >= 1) {
            Log.d(TAG, "🟢 Objeto distante ou pequeno movimento  blockDiff:$blockDiff")
        }

        /*
        if (blockDiff >= blockMotionThreshold) {
            motionCount++
            Log.d(TAG, "Possível movimento detectado (blocos diferentes = $blockDiff), motionCount=$motionCount")
            if (motionCount >= requiredMotionFrames) {
                Log.d(TAG, "Movimento confirmado!")

                motionCount = 0
                //onMotionDetected()
                val intent = Intent("com.ioline.OPEN_TARGET_APP")
                sendBroadcast(intent)
            }
        } else {
            motionCount = 0
        }

         */
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    // --- helpers para conversão YUV420 -> grayscale downsized ---
    private fun yuvToGray(image: Image, dstW: Int, dstH: Int): ByteArray {
        val yPlane = image.planes[0]
        val yBuf = yPlane.buffer
        val srcW = image.width
        val srcH = image.height
        // simples downscale por amostragem
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

    private fun computeFrameDiff(a: ByteArray, b: ByteArray): Int {
        var s = 0
        for (i in a.indices) {
            s += abs((a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF))
        }
        return s
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
                if (sum > 1000) blockChanges++ // só conta blocos com mudança significativa
            }
        }

        return blockChanges
    }



}