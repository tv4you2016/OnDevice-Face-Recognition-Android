package com.ioline.ithink.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ioline.ithink.ai.settingsdatastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d("BootReceiver", "${intent.action} recebido")

                // Cria instância do SettingsDataStore
                val settingsStore = SettingsDataStore(context)

                // Como DataStore é assíncrono, usamos CoroutineScope
                CoroutineScope(Dispatchers.IO).launch {
                    val openiThink = settingsStore.settingsFlow.first().OpeniThink.openApk

                    val intent2 = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("openiThink", openiThink) // envia na intent
                    }
                    Log.d("BootReceiver", "openiThink: $openiThink ")
                    context.startActivity(intent2)
                }
            }
        }
    }
}