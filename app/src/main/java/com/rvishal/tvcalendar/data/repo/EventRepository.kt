package com.rvishal.tvcalendar.data.repo

import com.rvishal.tvcalendar.data.db.EventDao
import com.rvishal.tvcalendar.data.db.EventEntity
import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource
import com.rvishal.tvcalendar.data.model.ItemKind
import com.rvishal.tvcalendar.data.prefs.SettingsStore
import com.rvishal.tvcalendar.data.sources.EventDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
class EventRepository(
    private val dao: EventDao,
    private val sources: Set<EventDataSource>,
    private val settings: SettingsStore,
) {
    /** Events overlapping [windowStart, windowEnd) for display. */
    fun observe(windowStart: Long, windowEnd: Long): Flow<List<CalendarEvent>> =
        dao.observe(windowStart, windowEnd).map { list ->
            list.map { it.toModel() }
                .filter { settings.isSourceEnabled(it.source.name) }
        }

    /** Sync window: previous month through +6 months. */
    fun syncWindow(): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -1)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 7)
        return start to cal.timeInMillis
    }

    /** Pulls every enabled source and merges into Room. Returns per-source errors. */
    suspend fun syncAll(): Map<String, String> {
        val (start, end) = syncWindow()
        val errors = mutableMapOf<String, String>()
        for (src in sources) {
            if (!src.isConfigured || !settings.isSourceEnabled(src.source.name)) continue
            try {
                val events = src.fetch(start, end)
                dao.deleteSource(src.source.name)
                dao.upsertAll(events.map { EventEntity.from(it) })
            } catch (e: Exception) {
                errors[src.source.label] = e.message ?: "sync failed"
            }
        }
        return errors
    }

    /** To-do created on the TV itself — stored locally, never synced anywhere. */
    suspend fun addLocalTodo(title: String, dayStartMillis: Long) {
        val start = utcMidnight(dayStartMillis)
        dao.upsert(
            EventEntity.from(
                CalendarEvent(
                    id = "local:${UUID.randomUUID()}",
                    source = EventSource.LOCAL,
                    calendarName = "My To-dos",
                    title = title,
                    startMillis = start,
                    endMillis = start + 1,
                    allDay = true,
                    kind = ItemKind.TODO,
                )
            )
        )
    }

    suspend fun setCompleted(id: String, completed: Boolean) = dao.setCompleted(id, completed)

    suspend fun deleteItem(id: String) = dao.delete(id)

    suspend fun todayEvents(): List<CalendarEvent> {
        val start = startOfDay(System.currentTimeMillis())
        return dao.inWindow(start, start + 86_400_000L).map { it.toModel() }
            .filter { settings.isSourceEnabled(it.source.name) }
    }

    private fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** UTC-midnight anchor for the local calendar day containing [millis]. */
    private fun utcMidnight(millis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = millis }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        return utc.timeInMillis
    }

    suspend fun clearAll() = dao.clear()
}
