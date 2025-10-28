package com.ioline.ithink.ai

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class TouchOverlay(context: Context, val onTouchDetected: () -> Unit) : View(context) {

    private var lastTouchTime = System.currentTimeMillis()
    private val interval: Long = 60_000 // 60 segundos de inatividad
    private val handler = Handler(Looper.getMainLooper())


    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    )

    fun show() {
        try {
            windowManager.addView(this, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide() {
        try {
            windowManager.removeView(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        onTouchDetected()
        return false
    }

    fun scheduleOverlayStart(context: Context) {
        val handler = Handler(Looper.getMainLooper())

        // Executa imediatamente o runnable de verificação
        handler.post {
            Log.d("TouchOverlay", "Verificação inicial do overlay")
        }

        // Aguarda 2 segundos antes de mostrar o overlay
        handler.postDelayed({
            val touchOverlay = TouchOverlay(context) {
                val lastTouchTime = System.currentTimeMillis()
                Log.d("TouchOverlay", "Toque detectado! Timer reiniciado. $lastTouchTime")
            }
            touchOverlay.show()
        }, 2000) // delay de 2 segundos após "acordar a tela"
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val elapsedMillis = now - lastTouchTime
            val elapsedSeconds = elapsedMillis / 1000

            Log.d("MonitorService", "🕒 Tempo desde o último toque: ${elapsedSeconds}s")


            if (elapsedMillis >= interval) {

                Log.d(
                    "MonitorService",
                    "🕒 Inatividade detectada (${elapsedSeconds}s)"
                )

                if (WakeLock().isScreenOn(context)) {
                    Log.d(
                        "MonitorService",
                        "Screen ON -> Abrir app..."
                    )
                    //openTargetApp(false)
                } else {
                    Log.d(
                        "MonitorService",
                        "Screen OFF ->"
                    )
                    //openTargetApp(false)
                }

                lastTouchTime = now
            }

            handler.postDelayed(this, 1000)
        }
    }


}
