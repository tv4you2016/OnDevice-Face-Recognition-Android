// AppUtils.kt
package com.ioline.aicamera.utils

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.app.ServiceCompat.startForeground
import com.ioline.ithink.ai.MyDeviceAdminReceiver
import com.ioline.ithink.ai.WakeLock

object AppUtils {

    fun openTargetApp(context: Context, wakeLock: Boolean) {
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





}
