package com.rvishal.tvcalendar.data.sources

import android.content.Context
import com.rvishal.tvcalendar.R
import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.TimeZone
class GoogleCalendarSource(
    private val context: Context,
    private val client: OkHttpClient,
    private val auth: GoogleAuthManager,
    private val settings: SettingsStore,
    private val json: Json,
) : EventDataSource {

    override val source = EventSource.GOOGLE
    override val isConfigured: Boolean get() = settings.isGoogleSignedIn

    private val rfc3339 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    @Serializable
    data class EventListResponse(
        val items: List<Item> = emptyList(),
        @SerialName("nextSyncToken") val nextSyncToken: String? = null,
        @SerialName("nextPageToken") val nextPageToken: String? = null,
    )

    @Serializable
    data class Item(
        val id: String,
        val summary: String? = null,
        val description: String? = null,
        val location: String? = null,
        val status: String = "confirmed",
        val start: EventTime? = null,
        val end: EventTime? = null,
        val colorId: String? = null,
        val organizer: Organizer? = null,
    )

    @Serializable
    data class Organizer(@SerialName("displayName") val displayName: String? = null)

    @Serializable
    data class EventTime(
        @SerialName("dateTime") val dateTime: String? = null,
        val date: String? = null,
        val timeZone: String? = null,
    )

    override suspend fun fetch(windowStartMillis: Long, windowEndMillis: Long): List<CalendarEvent> {
        val token = auth.validAccessToken(clientId()) ?: return emptyList()
        val out = mutableListOf<CalendarEvent>()
        var pageToken: String? = null
        do {
            val url = "https://www.googleapis.com/calendar/v3/calendars/primary/events".toHttpUrl()
                .newBuilder()
                .addQueryParameter("singleInstances", "true")
                .addQueryParameter("timeMin", rfc3339.format(windowStartMillis))
                .addQueryParameter("timeMax", rfc3339.format(windowEndMillis))
                .addQueryParameter("maxResults", "250")
                .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                .build()
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            val page = withContext(Dispatchers.IO) {
                client.newCall(req).execute().use { resp ->
                    check(resp.isSuccessful) { "calendar list failed: ${resp.code}" }
                    json.decodeFromString(EventListResponse.serializer(), resp.body!!.string())
                }
            }
            out += page.items.filter { it.status != "cancelled" }.mapNotNull { it.toEvent() }
            pageToken = page.nextPageToken
            page.nextSyncToken?.let { settings.googleSyncToken = it }
        } while (pageToken != null)
        return out
    }

    private fun Item.toEvent(): CalendarEvent? {
        val s = start ?: return null
        val e = end ?: return null
        val (startMs, allDay) = parseTime(s)
        val (endMs, _) = parseTime(e)
        return CalendarEvent(
            id = "gcal:$id",
            source = EventSource.GOOGLE,
            calendarName = "Google Calendar",
            title = summary ?: "(no title)",
            description = description,
            location = location,
            startMillis = startMs,
            endMillis = maxOf(endMs, startMs + 1),
            allDay = allDay,
        )
    }

    private fun parseTime(t: EventTime): Pair<Long, Boolean> {
        t.dateTime?.let { return parseIsoFallback(it) to false }
        val date = t.date ?: return 0L to true
        val df = SimpleDateFormat("yyyy-MM-dd").apply { timeZone = TimeZone.getTimeZone("UTC") }
        return (df.parse(date)?.time ?: 0L) to true
    }

    private fun parseIsoFallback(iso: String): Long {
        // Handles 2026-10-31T15:00:00+08:00 and ...Z
        val pattern = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        val tzStart = iso.lastIndexOf('+').takeIf { it > 10 }
            ?: iso.lastIndexOf('-').takeIf { it > 10 }
        return if (iso.endsWith("Z")) {
            pattern.apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(iso.removeSuffix("Z"))?.time ?: 0L
        } else if (tzStart != null) {
            val offsetStr = iso.substring(tzStart).replace(":", "")
            val sign = if (iso[tzStart] == '+') 1 else -1
            val hours = offsetStr.substring(1, 3).toIntOrNull() ?: 0
            val mins = offsetStr.substring(3, 5).toIntOrNull() ?: 0
            val offset = sign * (hours * 60 + mins) * 60_000L
            pattern.apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(iso.substring(0, tzStart))?.time?.minus(offset) ?: 0L
        } else {
            pattern.parse(iso)?.time ?: 0L
        }
    }

    private fun clientId(): String = context.getString(R.string.google_client_id)
}
