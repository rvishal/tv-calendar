package com.rvishal.tvcalendar.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rvishal.tvcalendar.di.AppGraph
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Sends the day's agenda to Telegram once a day (~08:00) when enabled in settings. */
class DailyDigestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = AppGraph.settings
        if (!settings.telegramDigestEnabled || !settings.isTelegramConfigured) return Result.success()
        return try {
            val events = AppGraph.repository.todayEvents()
            AppGraph.telegramDigest.send(events)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.w("DailyDigestWorker", "digest send failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "telegram-digest"
        private const val HOUR = 8

        /** Periodic work starting at the next 08:00 local. Call again when the toggle changes. */
        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val now = System.currentTimeMillis()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, HOUR); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<DailyDigestWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(next - now, TimeUnit.MILLISECONDS)
                    .build()
            )
        }
    }
}
