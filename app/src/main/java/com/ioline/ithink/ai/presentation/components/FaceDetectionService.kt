package com.ioline.ithink.ai.presentation.components

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.ioline.ithink.ai.domain.ImageVectorUseCase
import com.ioline.ithink.ai.domain.PersonUseCase
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.util.concurrent.Executors

@ExperimentalGetImage
class FaceDetectionService : Service() {


    private val personUseCase: PersonUseCase by inject()
    private val imageVectorUseCase: ImageVectorUseCase by inject()

    private var isProcessing = false
    private var isImageTransformInitialized = false
    private var imageTransform = Matrix()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startCamera()
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val executor = ContextCompat.getMainExecutor(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val frameAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            frameAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(/* lifecycleOwner = */ FakeLifecycleOwner(), cameraSelector, frameAnalyzer)
        }, executor)
    }

    private val analyzer = ImageAnalysis.Analyzer { image ->
        if (isProcessing) {
            image.close()
            return@Analyzer
        }

        isProcessing = true

        val bitmap = Bitmap.createBitmap(image.image!!.width, image.image!!.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(image.planes[0].buffer)

        if (!isImageTransformInitialized) {
            imageTransform = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
            isImageTransformInitialized = true
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0, 0, bitmap.width, bitmap.height, imageTransform, false
        )

        coroutineScope.launch {
            val (metrics, results) = imageVectorUseCase.getNearestPersonName(rotatedBitmap, false)

            results.forEach { (name, _, spoofResult) ->
                var personName = name
                val numPeople = personUseCase.getCount()
                if (numPeople.toInt() == 0) personName = ""
                if (spoofResult != null && spoofResult.isSpoof) {
                    personName = "$personName (Spoof: ${spoofResult.score})"
                }
                Log.i("IOLine", "Detectado: $personName")
            }

            withContext(Dispatchers.Main) {
                isProcessing = false
            }
        }

        image.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
