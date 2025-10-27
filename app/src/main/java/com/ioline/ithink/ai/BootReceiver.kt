package com.ioline.ithink.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d("BootReceiver", "${intent.action} recebido")

                val intent2 = Intent(context, MainActivity::class.java)
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(intent2)
            }
        }
    }
}