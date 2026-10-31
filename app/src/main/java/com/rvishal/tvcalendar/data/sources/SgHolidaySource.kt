package com.rvishal.tvcalendar.data.sources

import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.TimeZone
/**
 * Singapore public holidays from data.gov.sg (MOM). Yearly dataset IDs are
 * resolved from the collection metadata so nothing is hardcoded per year.
 */
class SgHolidaySource(
    private val client: OkHttpClient,
    private val json: Json,
) : EventDataSource {

    override val source = EventSource.SG_HOLIDAY
    override val isConfigured = true

    private val collectionUrl =
        "https://api-production.data.gov.sg/v2/public/api/collections/691/metadata"
    private val day = SimpleDateFormat("yyyy-MM-dd").apply {
        timeZone = TimeZone.getTimeZone("Asia/Singapore")
    }

    @Serializable
    data class CollectionResponse(val data: CollectionData? = null)

    @Serializable
    data class CollectionData(
        val collectionMetadata: CollectionMeta? = null,
    )

    @Serializable
    data class CollectionMeta(@SerialName("childDatasets") val childDatasets: List<String> = emptyList())

    @Serializable
    data class DatastoreResponse(val result: DatastoreResult? = null)

    @Serializable
    data class DatastoreResult(val records: List<Record> = emptyList())

    @Serializable
    data class Record(
        val date: String? = null,
        val day: String? = null,
        val holiday: String? = null,
        val year: String? = null,
    )

    override suspend fun fetch(windowStartMillis: Long, windowEndMillis: Long): List<CalendarEvent> {
        val datasetIds = datasetIds() ?: return emptyList()
        val out = mutableListOf<CalendarEvent>()
        for (id in datasetIds) {
            val url = "https://data.gov.sg/api/action/datastore_search?resource_id=$id&limit=200"
            val records = get<DatastoreResponse>(url)?.result?.records ?: continue
            for (r in records) {
                val date = r.date ?: continue
                val start = day.parse(date)?.time ?: continue
                if (start in windowStartMillis until windowEndMillis) {
                    out += CalendarEvent(
                        id = "sg:$date:${r.holiday}",
                        source = EventSource.SG_HOLIDAY,
                        calendarName = "SG Public Holidays",
                        title = r.holiday ?: "Public Holiday",
                        startMillis = start,
                        endMillis = start + 24 * 3600_000,
                        allDay = true,
                    )
                }
            }
        }
        return out
    }

    private suspend fun datasetIds(): List<String>? =
        get<CollectionResponse>(collectionUrl)?.data?.collectionMetadata?.childDatasets

    private suspend inline fun <reified T> get(url: String): T? = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                json.decodeFromString<T>(resp.body!!.string())
            }
        }.getOrNull()
    }
}
