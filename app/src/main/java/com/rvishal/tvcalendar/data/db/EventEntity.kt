package com.rvishal.tvcalendar.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource

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
) {
    fun toModel() = CalendarEvent(
        id = id,
        source = EventSource.valueOf(source),
        calendarName = calendarName,
        title = title,
        description = description,
        location = location,
        startMillis = startMillis,
        endMillis = endMillis,
        allDay = allDay,
        color = color,
    )

    companion object {
        fun from(e: CalendarEvent) = EventEntity(
            id = e.id, source = e.source.name, calendarName = e.calendarName,
            title = e.title, description = e.description, location = e.location,
            startMillis = e.startMillis, endMillis = e.endMillis,
            allDay = e.allDay, color = e.color,
        )
    }
}
