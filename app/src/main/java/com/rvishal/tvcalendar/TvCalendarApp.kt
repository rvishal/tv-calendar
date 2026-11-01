package com.rvishal.tvcalendar

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rvishal.tvcalendar.data.sync.DailyDigestWorker
import com.rvishal.tvcalendar.data.sync.SyncWorker
import com.rvishal.tvcalendar.di.AppGraph
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TvCalendarApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).build()
        )
        DailyDigestWorker.schedule(this, AppGraph.settings.telegramDigestEnabled)
        appScope.launch {
            if (AppGraph.settings.homeChannelId < 0) {
                AppGraph.homeChannel.ensureChannel()?.let { AppGraph.settings.homeChannelId = it }
            }
            val id = AppGraph.settings.homeChannelId
            if (id > 0) AppGraph.homeChannel.publishToday(id)
        }
    }
}
