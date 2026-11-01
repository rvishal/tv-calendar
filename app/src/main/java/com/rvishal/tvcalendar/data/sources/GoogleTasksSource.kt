package com.rvishal.tvcalendar.data.sources

import android.content.Context
import com.rvishal.tvcalendar.R
import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource
import com.rvishal.tvcalendar.data.model.ItemKind
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant

/** Google Tasks (read-only): every task list's tasks with a due date land on the calendar. */
class GoogleTasksSource(
    private val context: Context,
    private val client: OkHttpClient,
    private val auth: GoogleAuthManager,
    private val settings: SettingsStore,
    private val json: Json,
) : EventDataSource {

    override val source = EventSource.GTASK
    override val isConfigured: Boolean get() = settings.isGoogleSignedIn

    @Serializable data class TaskListsResponse(val items: List<TaskList> = emptyList())
    @Serializable data class TaskList(val id: String, val title: String = "Tasks")
    @Serializable data class TasksResponse(val items: List<Task> = emptyList())
    @Serializable data class Task(
        val id: String,
        val title: String = "",
        val status: String = "needsAction",
        val due: String? = null,
        val notes: String? = null,
        @SerialName("deleted") val deleted: Boolean = false,
        @SerialName("hidden") val hidden: Boolean = false,
    )

    override suspend fun fetch(windowStartMillis: Long, windowEndMillis: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val token = auth.validAccessToken(
                context.getString(R.string.google_client_id),
                context.getString(R.string.google_client_secret),
            ) ?: error("not signed in")

            val listsUrl = "https://tasks.googleapis.com/tasks/v1/users/@me/lists".toHttpUrl()
            val lists = get(listsUrl.toString(), token)
                ?: return@withContext emptyList()
            val taskLists = json.decodeFromString(TaskListsResponse.serializer(), lists).items

            val out = mutableListOf<CalendarEvent>()
            for (list in taskLists) {
                val url = "https://tasks.googleapis.com/tasks/v1/lists/${list.id}/tasks".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("showCompleted", "true")
                    .addQueryParameter("showHidden", "false")
                    .addQueryParameter("maxResults", "100")
                    .build().toString()
                val body = get(url, token) ?: continue
                for (task in json.decodeFromString(TasksResponse.serializer(), body).items) {
                    if (task.deleted || task.hidden) continue
                    val due = task.due?.let { Instant.parse(it).toEpochMilli() } ?: continue
                    if (due < windowStartMillis || due >= windowEndMillis) continue
                    out += CalendarEvent(
                        id = "gtask:${list.id}:${task.id}",
                        source = EventSource.GTASK,
                        calendarName = list.title,
                        title = task.title.ifBlank { "(no title)" },
                        description = task.notes,
                        startMillis = due,
                        endMillis = due + 1,
                        allDay = true,
                        kind = ItemKind.TODO,
                        completed = task.status == "completed",
                    )
                }
            }
            out
        }

    private fun get(url: String, token: String): String? {
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) return resp.body?.string()
            android.util.Log.w("GoogleTasksSource", "GET $url -> ${resp.code}")
            return null
        }
    }
}
