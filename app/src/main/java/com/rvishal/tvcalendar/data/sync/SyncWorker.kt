package com.rvishal.tvcalendar.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rvishal.tvcalendar.di.AppGraph

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val errors = AppGraph.repository.syncAll()
        return if (errors.size == 4) Result.retry() else Result.success()
    }

    companion object {
        const val WORK_NAME = "calendar-sync"

        fun enqueueNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
        }
    }
}
