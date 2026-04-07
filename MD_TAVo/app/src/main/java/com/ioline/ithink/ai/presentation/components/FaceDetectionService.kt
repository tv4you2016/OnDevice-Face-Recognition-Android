package com.ioline.ithink.ai.presentation.components

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
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
import androidx.core.graphics.createBitmap
import com.ioline.ithink.ai.AppUtils.openTargetAppSafe
import com.ioline.ithink.ai.WakeLock
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.flow.first




@ExperimentalGetImage
class FaceDetectionService : Service() {


    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val settingsStore by lazy { SettingsDataStore(this) }

    // Flag interna para evitar abrir várias vezes
    private var hasOpenedTargetApp = false

    private val personUseCase: PersonUseCase by inject()
    private val imageVectorUseCase: ImageVectorUseCase by inject()

    private var isProcessing = false
    private var isImageTransformInitialized = false
    private var imageTransform = Matrix()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())



    // 🔹 Guarda referência ao cameraProvider
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onBind(intent: Intent?): IBinder? = null




    companion object {
        private const val ACTION_FACE_READY = "com.ioline.ithink.ai.action.FACE_READY"


        private const val ACTION_PAUSE_CAMERA = "com.ioline.ithink.ai.action.PAUSE_FACE_CAMERA"
        private const val ACTION_RESUME_CAMERA = "com.ioline.ithink.ai.action.RESUME_FACE_CAMERA"

        fun pauseCamera(context: Context) {
            context.startService(Intent(context, FaceDetectionService::class.java).apply {
                action = ACTION_PAUSE_CAMERA
            })
        }

        fun resumeCamera(context: Context) {
            context.startService(Intent(context, FaceDetectionService::class.java).apply {
                action = ACTION_RESUME_CAMERA
            })
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FaceDetectionService::class.java))
        }
    }

    private fun notifyReady() {
        sendBroadcast(Intent(ACTION_FACE_READY))
        Log.d("FaceDetectionService", "READY broadcast sent")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE_CAMERA -> {
                cameraProvider?.unbindAll()
                isProcessing = false
                isImageTransformInitialized = false
            }
            ACTION_RESUME_CAMERA -> startCamera()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("FaceDetectionService", "onCreate")
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
            cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val frameAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            frameAnalyzer.setAnalyzer(cameraExecutor, analyzer)

            cameraProvider?.unbindAll()
            try {
                cameraProvider?.bindToLifecycle(FakeLifecycleOwner(), cameraSelector, frameAnalyzer)
                notifyReady()
            } catch (t: Throwable) {
                Log.e("FaceDetectionService", "bindToLifecycle failed", t)
                notifyReady() // ou manda outra action de erro
            }
        }, executor)

    }

    private val analyzer = ImageAnalysis.Analyzer { image ->
        if (isProcessing) {
            image.close()
            return@Analyzer
        }

        isProcessing = true

        val bitmap = createBitmap(image.image!!.width, image.image!!.height)
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

                val numPeople = personUseCase.getCount()

                val originalName = if (numPeople.toInt() == 0) "" else name
                val isRecognized = originalName.isNotEmpty() && originalName != "Not recognized"
                val isSpoof = spoofResult?.isSpoof == true

                // Só para log (não mexe na lógica)
                val logName = if (isSpoof) {
                    "$originalName (Spoof: ${spoofResult?.score})"
                } else {
                    originalName
                }

                Log.i("IOLine", "Detectado: $logName")

                // 🔥 REGRA FINAL
                if (isRecognized && !isSpoof) {

                    coroutineScope.launch(Dispatchers.Main) {
                        val openiThink = settingsStore.settingsFlow.first().OpeniThink.openApk

                        if (openiThink && !hasOpenedTargetApp) {
                            hasOpenedTargetApp = true
                            WakeLock().wakeUpScreen(applicationContext)

                            openTargetAppSafe(this@FaceDetectionService, "app.ioline.ithink")

                            launch {
                                delay(5000)
                                hasOpenedTargetApp = false
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                isProcessing = false
            }
        }

        image.close()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("FaceDetectionService", "onDestroy")


        // 🔹 Fecha a câmera ao parar o serviço
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraExecutor.shutdown()

        stopForeground(STOP_FOREGROUND_REMOVE)
        coroutineScope.cancel()
        Log.i("FaceDetectionService", "Serviço parado")

    }
}
