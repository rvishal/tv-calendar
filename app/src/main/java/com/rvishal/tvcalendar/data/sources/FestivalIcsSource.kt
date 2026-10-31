package com.rvishal.tvcalendar.data.sources

import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import com.rvishal.tvcalendar.util.IcsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
/**
 * Indian religious festivals from a configurable ICS feed. Defaults to Google's
 * public "Hindu Festivals" calendar which covers Karnataka festivals
 * (Ugadi, Varamahalakshmi Vrata, Ayudha Puja, Balipadyami, Deepavali...).
 * A Karnataka-specific feed URL can be set in Settings.
 */
class FestivalIcsSource(
    private val client: OkHttpClient,
    private val settings: SettingsStore,
) : EventDataSource {

    override val source = EventSource.FESTIVAL
    override val isConfigured = true

    override suspend fun fetch(windowStartMillis: Long, windowEndMillis: Long): List<CalendarEvent> {
        val url = settings.festivalIcsUrl.replace("webcal://", "https://").trim()
        if (url.isBlank()) return emptyList()
        val ics = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
            }.getOrNull()
        } ?: return emptyList()
        return IcsParser.parse(ics, EventSource.FESTIVAL, "Hindu Festivals", windowStartMillis, windowEndMillis)
    }
}
