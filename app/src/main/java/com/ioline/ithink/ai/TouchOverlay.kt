package com.ioline.ithink.ai

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class TouchOverlay(context: Context, val onTouchDetected: () -> Unit) : View(context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val layoutParams = WindowManager.LayoutParams(
        1, // largura mínima
        1, // altura mínima
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
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


    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("TouchOverlay", "🖐 Toque detectado! Resetando timer do serviço...")
        context.sendBroadcast(Intent("RESET_PERIODIC_TIMER"))
        onTouchDetected()
        return false // evento continua para o app
    }



}
