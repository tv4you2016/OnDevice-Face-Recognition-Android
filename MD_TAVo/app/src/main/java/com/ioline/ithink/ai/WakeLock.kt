package com.ioline.ithink.ai

import android.content.Context
import android.os.PowerManager
import android.app.KeyguardManager
import android.util.Log

class WakeLock {
    fun wakeUpScreen(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "ithink:wakelock"
        )

        wakeLock.acquire(3000) // 3 segundos
        wakeLock.release()
    }

    fun isScreenOn(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    fun unlockScreen(context: Context) {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val keyguardLock = km.newKeyguardLock("wakeupmd:KeyguardLock")

        try {
            // Para versões antigas
            @Suppress("DEPRECATION")
            keyguardLock.disableKeyguard()
            Log.d("WakeLock", "Keyguard disabled")
        } catch (e: Exception) {
            Log.e("WakeLock", "Failed to disable keyguard: ${e.message}")
        }

        if (km.isKeyguardLocked) {
            Log.d("WakeLock", "Keyguard ainda ativo (provável bloqueio seguro)")
        } else {
            Log.d("WakeLock", "Keyguard removido com sucesso")
        }
    }


}
