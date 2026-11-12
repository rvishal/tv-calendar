package com.rvishal.tvcalendar.util

import com.rvishal.tvcalendar.data.model.CalendarEvent
import com.rvishal.tvcalendar.data.model.EventSource
import com.rvishal.tvcalendar.data.model.ItemKind
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.component.VToDo
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.util.MapTimeZoneCache
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.TimeZone

object IcsParser {

    init {
        // Avoids ical4j hitting the network for timezone definitions.
        System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache::class.java.name)
    }

    /**
     * Parses ICS text into events overlapping [windowStart, windowEnd), expanding RRULE
     * recurrences inside the window.
     */
    fun parse(
        icsText: String,
        source: EventSource,
        calendarName: String,
        windowStart: Long,
        windowEnd: Long,
    ): List<CalendarEvent> {
        val window = Period(
            net.fortuna.ical4j.model.DateTime(windowStart),
            net.fortuna.ical4j.model.DateTime(windowEnd),
        )
        val calendar = CalendarBuilder().build(StringReader(icsText))
        val out = mutableListOf<CalendarEvent>()
        for (component in calendar.getComponents<VEvent>(VEvent.VEVENT)) {
            val periods: Collection<Period> = try {
                component.calculateRecurrenceSet(window)
            } catch (e: Exception) {
                emptyList()
            }
            val allDay = component.getProperty<DtStart>("DTSTART")?.let {
                it.toString().contains("VALUE=DATE")
            } ?: false
            for (p in periods) {
                out += CalendarEvent(
                    id = "${source.name.lowercase()}:${component.uid?.value}:${p.start.time}",
                    source = source,
                    calendarName = calendarName,
                    title = component.summary?.value ?: "(no title)",
                    description = component.description?.value,
                    location = component.location?.value,
                    startMillis = p.start.time,
                    endMillis = maxOf(p.end.time, p.start.time + 1),
                    allDay = allDay,
                )
            }
        }
        return out
    }

    /**
     * Parses VTODO components (Reminders / tasks) from ICS text. Each todo lands on
     * its DUE date (falling back to DTSTART); undated todos anchor to today so
     * they stay visible — iCloud Reminders are mostly undated. Completed = done.
     */
    fun parseTodos(
        icsText: String,
        source: EventSource,
        calendarName: String,
        windowStart: Long,
        windowEnd: Long,
    ): List<CalendarEvent> {
        val todayAnchor = Dates.localDayUtcAnchor(System.currentTimeMillis())
        val calendar = CalendarBuilder().build(StringReader(icsText))
        val out = mutableListOf<CalendarEvent>()
        for (todo in calendar.getComponents<VToDo>(VToDo.VTODO)) {
            val dated = todo.due?.date?.time
                ?: todo.getProperty<DtStart>("DTSTART")?.date?.time
            val due = dated ?: todayAnchor
            if (due < windowStart || due >= windowEnd) continue
            out += CalendarEvent(
                id = "${source.name.lowercase()}:todo:${todo.uid?.value}",
                source = source,
                calendarName = calendarName,
                title = todo.summary?.value ?: "(no title)",
                description = todo.description?.value,
                startMillis = due,
                endMillis = due + 1,
                allDay = true,
                kind = ItemKind.TODO,
                completed = todo.status?.value == "COMPLETED",
                undated = dated == null,
            )
        }
        return out
    }

    fun parseAllDayDate(value: String): Long =
        SimpleDateFormat("yyyyMMdd").apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(value)?.time ?: 0L
}
