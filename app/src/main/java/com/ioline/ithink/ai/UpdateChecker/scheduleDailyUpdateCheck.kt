package com.ioline.ithink.ai.UpdateChecker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun scheduleDailyUpdateCheck(context: Context) {
/*
    val workRequest = PeriodicWorkRequestBuilder<CheckUpdateWorker>(
        8, TimeUnit.HOURS
    ).build()

*/

    val workRequest = PeriodicWorkRequestBuilder<CheckUpdateWorker>(
        8, TimeUnit.HOURS
    ).build()

    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(
            "daily_check_update",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
}