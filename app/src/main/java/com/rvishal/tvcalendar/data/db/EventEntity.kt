package com.rvishal.tvcalendar.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource
import com.rvishal.tvcalendar.data.model.ItemKind
import com.rvishal.tvcalendar.util.Dates

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val source: String,
    val calendarName: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val color: Long?,
    val kind: String,
    val completed: Boolean,
    val undated: Boolean = false,
) {
    fun toModel(): CalendarEvent {
        // Undated open todos carry today's anchor so they don't go stale between syncs.
        val float = kind == ItemKind.TODO.name && undated && !completed
        val start = if (float) Dates.localDayUtcAnchor(System.currentTimeMillis()) else startMillis
        return CalendarEvent(
            id = id,
            source = EventSource.valueOf(source),
            calendarName = calendarName,
            title = title,
            description = description,
            location = location,
            startMillis = start,
            endMillis = if (float) start + 1 else endMillis,
            allDay = allDay,
            color = color,
            kind = runCatching { ItemKind.valueOf(kind) }.getOrDefault(ItemKind.EVENT),
            completed = completed,
            undated = undated,
        )
    }

    companion object {
        fun from(e: CalendarEvent) = EventEntity(
            id = e.id, source = e.source.name, calendarName = e.calendarName,
            title = e.title, description = e.description, location = e.location,
            startMillis = e.startMillis, endMillis = e.endMillis,
            allDay = e.allDay, color = e.color,
            kind = e.kind.name, completed = e.completed, undated = e.undated,
        )
    }
}
