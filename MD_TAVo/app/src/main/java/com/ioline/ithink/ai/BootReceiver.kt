package com.ioline.ithink.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d("BootReceiver", "Recebido: ${intent.action}")

                val prefs = context.getSharedPreferences("boot_flags", Context.MODE_PRIVATE)
                prefs.edit { putBoolean("pending_open_ithink_after_boot", true) }

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }

                try {
                    context.startActivity(launchIntent)
                    Log.d("BootReceiver", "MainActivity lançada no boot")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Erro ao abrir MainActivity no boot", e)
                }
            }
        }
    }
}