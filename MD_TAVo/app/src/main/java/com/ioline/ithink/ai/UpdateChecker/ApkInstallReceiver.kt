package com.ioline.ithink.ai.UpdateChecker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import java.io.File

class ApkInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {

            val pkg = intent.data?.schemeSpecificPart ?: return

            // Remova só se for o seu app
            if (!pkg.contains("ioline", ignoreCase = true)) return

            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

            downloadsDir?.listFiles()?.forEach { file ->
                if (
                    file.isFile &&
                    file.name.contains("mordomus_tavo", ignoreCase = true) &&
                    file.name.endsWith(".apk")
                ) {
                    file.delete()
                }
            }
        }
    }
}
