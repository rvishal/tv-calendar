package com.rvishal.tvcalendar.data.sync

import android.content.Context
import android.content.Intent
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.rvishal.tvcalendar.R
import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.ItemKind
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import com.rvishal.tvcalendar.data.repo.EventRepository
import com.rvishal.tvcalendar.util.Dates
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Publishes today's agenda as a "TV Calendar" channel on the Android TV home screen. */
class HomeChannel(
    private val context: Context,
    private val settings: SettingsStore,
    private val repository: EventRepository,
) {

    /** Ensures the channel exists and asks the launcher to display it. Returns the channel id. */
    fun ensureChannel(): Long? {
        val channel = Channel.Builder()
            .setType(TvContractCompat.Channels.TYPE_PREVIEW)
            .setDisplayName(context.getString(R.string.app_name))
            .setBrowsable(true)
            .build()
        return try {
            val id = context.contentResolver.insert(TvContractCompat.Channels.CONTENT_URI, channel.toContentValues())
                ?.let { android.content.ContentUris.parseId(it) } ?: return null
            context.contentResolver.notifyChange(TvContractCompat.Channels.CONTENT_URI, null)
            try {
                val logo = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.banner)
                ChannelLogoUtils.storeChannelLogo(context, id, logo)
            } catch (_: Exception) { }
            TvContractCompat.requestChannelBrowsable(context, id)
            id
        } catch (e: Exception) {
            android.util.Log.w("HomeChannel", "channel create failed", e)
            null
        }
    }

    /** Deletes our preview programs and republishes from today's events. */
    suspend fun publishToday(channelId: Long) {
        val events = repository.todayEvents()
            .sortedBy { it.startMillis }
            .take(20)
        try {
            // The provider rejects query/delete selections entirely, so we track
            // the program IDs we insert and delete each by URI on the next publish.
            for (id in settings.homeChannelProgramIds) {
                runCatching {
                    context.contentResolver.delete(
                        TvContractCompat.buildPreviewProgramUri(id.toLong()), null, null,
                    )
                }
            }
            settings.homeChannelProgramIds = emptySet()
            val newIds = mutableSetOf<String>()
            events.forEachIndexed { i, e ->
                insertProgram(channelId, i, e)?.let { newIds += it.toString() }
            }
            settings.homeChannelProgramIds = newIds
        } catch (e: Exception) {
            android.util.Log.w("HomeChannel", "publish failed", e)
        }
    }

    private fun insertProgram(channelId: Long, pos: Int, event: CalendarEvent): Long? {
        val title = when {
            event.kind == ItemKind.TODO -> (if (event.completed) "✓ " else "○ ") + event.title
            event.allDay -> event.title
            else -> "${Dates.time(event.startMillis, settings.use24Hour)} — ${event.title}"
        }
        val program = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setTitle(title)
            .setDescription(event.calendarName)
            .setPosterArtUri(android.net.Uri.parse("android.resource://${context.packageName}/${R.drawable.banner}"))
            .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
            .setWeight(pos)
            .setIntentUri(android.net.Uri.parse(launchIntentUri(event)))
            .build()
        return runCatching {
            context.contentResolver.insert(
                TvContractCompat.PreviewPrograms.CONTENT_URI, program.toContentValues(),
            )?.let { android.content.ContentUris.parseId(it) }
        }.getOrNull()
    }

    private fun launchIntentUri(event: CalendarEvent): String {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(context.packageName, "com.rvishal.tvcalendar.MainActivity")
            data = android.net.Uri.parse("tvcalendar://day/${event.startMillis}")
        }
        return intent.toUri(Intent.URI_INTENT_SCHEME)
    }

    companion object {
        fun todayLabel(): String = SimpleDateFormat("EEEE d MMM", Locale.getDefault()).format(Date())
    }
}
