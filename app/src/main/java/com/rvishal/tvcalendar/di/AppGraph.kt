package com.rvishal.tvcalendar.di

import android.content.Context
import androidx.room.Room
import com.rvishal.tvcalendar.data.db.AppDatabase
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import com.rvishal.tvcalendar.data.repo.EventRepository
import com.rvishal.tvcalendar.data.sources.CalDavSource
import com.rvishal.tvcalendar.data.sources.EventDataSource
import com.rvishal.tvcalendar.data.sources.FestivalIcsSource
import com.rvishal.tvcalendar.data.sources.GoogleAuthManager
import com.rvishal.tvcalendar.data.sources.GoogleCalendarSource
import com.rvishal.tvcalendar.data.sources.SgHolidaySource
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-rolled dependency graph — keeps the build free of DI plugins. */
object AppGraph {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val json: Json by lazy { Json { ignoreUnknownKeys = true; coerceInputValues = true } }

    val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val db: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "tvcalendar.db").build()
    }

    val googleAuth: GoogleAuthManager by lazy { GoogleAuthManager(okHttp, settings, json) }

    val sources: List<EventDataSource> by lazy {
        listOf(
            GoogleCalendarSource(appContext, okHttp, googleAuth, settings, json),
            CalDavSource(okHttp, settings),
            SgHolidaySource(okHttp, json),
            FestivalIcsSource(okHttp, settings),
        )
    }

    val repository: EventRepository by lazy {
        EventRepository(db.eventDao(), sources.toSet(), settings)
    }
}
