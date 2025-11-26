package com.ioline.ithink.ai.UpdateChecker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CheckUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Pega o versionCode atual
            val packageManager = applicationContext.packageManager
            val packageName = applicationContext.packageName
            val currentVersionCode = packageManager
                .getPackageInfo(packageName, 0)
                .longVersionCode
                .toInt()

            // Chama o teu UpdateChecker
            val result = UpdateChecker(applicationContext).checkForUpdate(currentVersionCode)

            result.onSuccess { updateResult ->
                when (updateResult) {
                    is UpdateResult.UpdateAvailable -> {
                        Log.d("CheckUpdateWorker", "Há update disponível!")

                        // 🔔 Aqui o ideal é mandar uma notificação (melhor UX)
                        // Se quiseres mesmo abrir a Activity:
                        val intent = Intent(applicationContext, UpdaterActivity::class.java).apply {
                            putExtra("apkUrl", updateResult.url)
                            putExtra("latestVersionName", updateResult.version)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        applicationContext.startActivity(intent)
                    }
                    is UpdateResult.AlreadyUpToDate -> {
                        Log.d("CheckUpdateWorker", "App já está atualizada.")
                    }
                    else -> {}
                }
            }.onFailure { e ->
                Log.e("CheckUpdateWorker", "Erro ao verificar update: ${e.message}")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("CheckUpdateWorker", "Erro no Worker: ${e.message}")
            Result.retry()
        }
    }
}
