package com.rvishal.tvcalendar.data.sync

import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.ItemKind
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import com.rvishal.tvcalendar.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Sends the daily agenda digest to a Telegram chat via the Bot API. */
class TelegramDigest(
    private val client: OkHttpClient,
    private val settings: SettingsStore,
    private val json: Json,
) {

    @Serializable private data class SendResponse(val ok: Boolean = false, val description: String? = null)

    fun format(events: List<CalendarEvent>, use24h: Boolean): String {
        val today = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
        if (events.isEmpty()) return "📅 $today\nNo events today."
        val lines = events.sortedBy { it.startMillis }.map { e ->
            when {
                e.kind == ItemKind.TODO -> (if (e.completed) "☑ " else "☐ ") + e.title
                e.allDay -> "• ${e.title} (all day)"
                else -> "• ${Dates.time(e.startMillis, use24h)} ${e.title}"
            }
        }
        return "📅 $today\n" + lines.joinToString("\n")
    }

    suspend fun send(events: List<CalendarEvent>): String = withContext(Dispatchers.IO) {
        val token = settings.telegramBotToken!!.trim()
        val chatId = settings.telegramChatId!!.trim()
        val body = buildJsonObject {
            put("chat_id", chatId)
            put("text", format(events, settings.use24Hour))
        }.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("https://api.telegram.org/bot$token/sendMessage")
            .post(body).build()
        client.newCall(req).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("telegram send failed: ${resp.code} $respBody")
            val parsed = runCatching {
                json.decodeFromString(SendResponse.serializer(), respBody)
            }.getOrNull()
            if (parsed?.ok == false) error("telegram send failed: ${parsed.description}")
            "sent"
        }
    }
}
