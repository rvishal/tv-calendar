package com.rvishal.tvcalendar

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rvishal.tvcalendar.data.sync.SyncWorker
import com.rvishal.tvcalendar.di.AppGraph
import java.util.concurrent.TimeUnit

class TvCalendarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).build()
        )
    }
}
