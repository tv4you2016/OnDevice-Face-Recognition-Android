package com.ioline.ithink.ai.UpdateChecker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun scheduleDailyUpdateCheck(context: Context) {
    val wm = WorkManager.getInstance(context)

    // 1) corre já (uma vez)
    val runNow = OneTimeWorkRequestBuilder<CheckUpdateWorker>()
        .addTag("check_update_now")
        .build()

    wm.enqueueUniqueWork(
        "check_update_now",
        ExistingWorkPolicy.REPLACE,   // força executar de novo no arranque
        runNow
    )

    // 2) agenda periódico (8h)
    val periodic = PeriodicWorkRequestBuilder<CheckUpdateWorker>(8, TimeUnit.HOURS)
        .addTag("daily_check_update")
        .build()

    wm.enqueueUniquePeriodicWork(
        "daily_check_update",
        ExistingPeriodicWorkPolicy.UPDATE,
        periodic
    )
}